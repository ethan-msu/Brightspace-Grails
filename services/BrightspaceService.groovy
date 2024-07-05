package edu.missouristate.university.brightspace

import grails.gorm.transactions.Transactional
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import edu.missouristate.common.DESCodec
import grails.util.Environment

@Transactional
class BrightspaceService {

    def sqlService, errorService, mailService, personService, misService

    def D2L_URL = "<your.d2l.id.here>"

    def getAuthToken() {
        //get the token from the database
        //also handles token refreshes
        //omitted for security reasons if you have additional questions email ethanlynch@missouristate.edu
        /***
            returns: ['access_token': <access_token>, 'refresh_token': <refresh_token>]
        */
    }

    def replaceRefreshToken(def newToken){
        //puts the token that was in use back into the db
        //omitted for security reasons if you have additional questions email ethanlynch@missouristate.edu
        /***
            returns : true if insert was successful, otherwise false
        */
    }

    def addUserToCourse(int course,def user, int role){
        def authTokens = getAuthToken()
        if(authTokens){
            def payload = [
                "OrgUnitId": course,
                "UserId": user,
                "RoleId": role
            ]
            String payloadJSON = JsonOutput.toJson(payload)

            String apiUrl = '/d2l/api/lp/1.45/enrollments/'
            def uri = new URI(D2L_URL+apiUrl)
            HttpClient client = HttpClient.newHttpClient()
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJSON))
                    .header("Content-Type","application/json")
                    .header("Authorization","Bearer ${authTokens.access_token}")
                    .build()
            String connectionResponse = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
            replaceRefreshToken(authTokens?.refresh_token)
            def parsedResponse = new JsonSlurper().parseText(connectionResponse)
            if(parsedResponse?.Errors){
                errorService?.logError("Error adding user ${user} to course ${course} in brightspaceService.addUserToCourse. Errors ${JsonOutput.toJson(parsedResponse?.Errors)}")
            }else{
                addUserToCourseDB(course,user,role)
            }
            return parsedResponse
        }
    }

    def addUserToCourseDB(int org_unit_id, def d2lID, int role_code){
        def insertStatement = """insert into beardata.brightspace_enrollments(parent_code,child_code,role_name) values(:course,:mnum,:role)"""
        def mnum = getMnumByD2LID(d2lID)
        def course = getCourseCodeByOrgUnitId(org_unit_id)
        def role = getRole(role_code)
        return sqlService?.execute(insertStatement,[mnum:mnum,course:course,role:role])?:null
    }

    def getRole(role_code){
        def query = """select substr(CUSTOM_LOOKUP_VALUE_DESC,1,20) ROLE
                        from beardata.mis_custom_lookup_values
                        inner join beardata.mis_custom_lookup_types on CUSTOM_LOOKUP_TYPE_ID = MIS_CUSTOM_LOOKUP_TYPE_ID and custom_lookup_type = 'Brightspace Roles'
                    where CUSTOM_LOOKUP_VALUE = :role_code"""

        return sqlService?.firstRow(query,[role_code:role_code])?.ROLE?:null
    }

    def getCourseCodeByOrgUnitId(org_unit_id){
        def query = """select code from beardata.bsp_course_offering where org_unit_id = :org_unit_id
                        UNION
                        select code from beardata.brightspace_course_sections where org_unit_id = :org_unit_id"""
        return sqlService?.firstRow(query,[org_unit_id:org_unit_id])?.code?:null
    }

    def getCourseNameByOrgUnitId(org_unit_id){
        def query = """select NAME from beardata.bsp_course_offering where org_unit_id = :org_unit_id
                        UNION
                        select NAME from beardata.brightspace_course_sections where org_unit_id = :org_unit_id"""
        return sqlService?.firstRow(query,[org_unit_id:org_unit_id])?.NAME?:null
    }

    def whoami(){
        def authTokens = getAuthToken()
        if(authTokens){
            def apiUrl = 'https://missourisutest.brightspace.com/d2l/api/lp/1.45/users/whoami'
            HttpClient client = HttpClient.newHttpClient()
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl.toString()))
                    .version(HttpClient.Version.HTTP_1_1)
                    .GET()
                    .header("Accept", "application/vnd.hedtech.integration.v12+json")
                    .header("Authorization", "Bearer ${authTokens?.access_token}")
                    .build()
            String connectionResponse = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
            replaceRefreshToken(authTokens?.refresh_token)
            if(connectionResponse){
                def parsedResponse = new JsonSlurper().parseText(connectionResponse)
                return parsedResponse
            }


        }
    }

    def getD2LID(mnum){
        def query = """select D2L_ID from beardata.BSP_USERS where org_defined_id = :mnum"""
        def result = sqlService?.firstRow(query,[mnum:mnum])
        return result?.D2L_ID?:null
    }

    def getMnumByD2LID(d2lID){
        def query = """select org_defined_id from beardata.BSP_USERS where D2L_ID = :d2lID"""
        def result = sqlService?.firstRow(query,[d2lID:d2lID])
        return result?.org_defined_id?:null
    }

    def getCreatedCourses(d2lid){
        //This isn't strictly the courses just created by this user, we also always grab all the community courses since
        // they aren't really owned by a single user. we do store who requested it, but thats just for historical reference

        def query = """select count(*) count_courses, course_type from beardata.BSP_USER_COURSES where d2l_id = :d2l_id and course_type in ('Sandbox','Source') group by course_type
                            union
                            select count(*) count_courses, course_type from beardata.BSP_USER_COURSES where course_type = 'Community' group by course_type"""
        def result =  sqlService?.rows(query,[d2l_id:d2lid])
        def source = 0
        def sandbox = 0
        def community = 0
        if (result){
            result.each{
                if(it?.course_type == 'Source'){
                    source = it?.count_courses
                }
                if(it?.course_type == 'Sandbox'){
                    sandbox = it?.count_courses
                }
                if(it?.course_type == 'Community'){
                    community = it?.count_courses
                }
            }
        }
        return ["Source":source,"Sandbox":sandbox,"Community":community]

    }

    def addCourseToDB(String d2lid,String courseName,String courseType,int orgUnitId){
        def query = """insert into beardata.BSP_USER_COURSES(d2l_id,course_name,course_type,ORG_UNIT_ID) 
                            values(:d2lid,:courseName,:courseType,:orgUnitId)"""
        sqlService?.execute(query,[d2lid:d2lid,courseName:courseName,courseType:courseType,orgUnitId:orgUnitId])
    }

    def getBspCoursesByInstructor (profPidm,term){
        def profMnum = personService?.getBearPassNumberByPidm(profPidm)
        def profD2lID = getD2LID(profMnum)
        def academicCoursesQuery = """select 
            regexp_substr(course_offerings.code,'\\d*') CRN, --to check student enrollment
            course_offerings.org_unit_id ORG_UNIT_ID, --to enroll if allowed
            course_offerings.name NAME, --so the user can see what they're doing
            regexp_substr(course_offerings.code,'SU\\d{2}|SP\\d{2}|FA\\d{2}') TERM
        from beardata.bsp_course_offering course_offerings
            inner join beardata.brightspace_enrollments enrollments on enrollments.parent_code = course_offerings.code
            where enrollments.child_code = :mnum
            and regexp_like(course_offerings.code,'^\\d*-\\w{4}(?:_cs)?')
            and regexp_substr(course_offerings.code,'SU\\d{2}|SP\\d{2}|FA\\d{2}') = :term   
            and course_merged_into is null"""
        def academicCourses = sqlService?.rows(academicCoursesQuery,[mnum:profMnum,term:term])
        def nonacademicCoursesQuery = """select 
                                                    buc.ORG_UNIT_ID ORG_UNIT_ID,
                                                    bco.name NAME
                                                from beardata.BSP_USER_COURSES buc
                                                    left join beardata.bsp_course_offering bco on bco.org_unit_id = buc.org_unit_id
                                                where d2l_id = :profD2lID"""
        def nonacademicCourses = sqlService?.rows(nonacademicCoursesQuery,[profD2lID:profD2lID])
        return [academicCourses:academicCourses,nonacademicCourses:nonacademicCourses]
    }

    def isStudentInCourse(pidm,crn,term){
        if(/SU\d{2}|SP\d{2}|FA\d{2}/==~term){
            if(term.substring(0,2)=="SP"){
                term = '20'+term.substring(2)+'20'
            }else if(term.substring(0,2)=="SU"){
                term = '20'+term.substring(2)+'30'
            }else{
                term = '20'+term.substring(2)+'40'
            }

        }
        def query = """select count('a') as matches from sfrstcr 
                            where sfrstcr_pidm = :pidm 
                                and sfrstcr_crn = :crn
                                and sfrstcr_term_code = :term"""
        def m = sqlService?.firstRow(query,[pidm:pidm,crn:crn,term:term])?.matches
        return (m?:0)>0
    }

    def isStudentInCourseWP(pidm,crn,term){
        //stupid regex, but it's more readable for junior devs ¯\_(ツ)_/¯
        if(/SU\d{2}|SP\d{2}|FA\d{2}/==~term){
            if(term.substring(0,2)=="SP"){
                term = '20'+term.substring(2)+'20'
            }else if(term.substring(0,2)=="SU"){
                term = '20'+term.substring(2)+'30'
            }else{
                term = '20'+term.substring(2)+'40'
            }

        }
        term = sgfTermToWPTerm(term)
        def query = """select count('a') as matches from sfrstcr@APOLLO
                            where sfrstcr_pidm = :pidm 
                                and sfrstcr_crn = :crn
                                and sfrstcr_term_code = :term"""
        def m = sqlService?.firstRow(query,[pidm:pidm,crn:crn,term:term])?.matches
        return (m?:0)>0
    }

    def sgfTermToWPTerm(String sgfTerm){
        def wpTerm = ''
        if (sgfTerm.substring(4) == '40'){
            wpTerm = (Integer.parseInt(sgfTerm?.substring(0,3))+1)?.toString()+"10"
        }else{
            wpTerm = sgfTerm
        }
        return wpTerm

    }

    def getNearTerms(){
        def termCodes = misService?.getSurroundingTerms(3,null,3)?.code
        def terms = []
        termCodes?.each{ term->
            if(term.substring(4,6)=='20'){
                terms.add("SP${term.substring(2,4)}")
            }else if(term.substring(4,6)=='30'){
                terms.add("SU${term.substring(2,4)}")
            }else if(term.substring(4,6)=='40'){
                terms.add("FA${term.substring(2,4)}")
            }
        }

        return terms?:[]
    }

    def getUsersForCourse(org_unit_id){
        def query = """   select child_code, spriden_last_name||', '||spriden_first_name NAME, role_name ROLE
                                from beardata.brightspace_enrollments enrollments
                                    inner join beardata.bsp_course_offering offerings 
                                        on enrollments.parent_code = offerings.code
                                    left join saturn.spriden 
                                        on spriden_change_ind is null 
                                            and spriden_id = enrollments.child_code
                                where offerings.org_unit_id = :org_unit_id
                                and role_name not in ('Instructor','Learner')"""
        return sqlService?.rows(query,[org_unit_id:org_unit_id])?:null
    }

    def removeUserFromCourse(d2l_id, org_unit_id){
        def authTokens = getAuthToken()
        if(authTokens){
            String apiUrl = "/d2l/api/lp/1.45/enrollments/orgUnits/${org_unit_id}/users/${d2l_id}"
            def uri = new URI(D2L_URL+apiUrl)
            HttpClient client = HttpClient.newHttpClient()
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .DELETE()
                    .header("Content-Type","application/json")
                    .header("Authorization","Bearer ${authTokens.access_token}")
                    .build()
            String connectionResponse = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
            replaceRefreshToken(authTokens?.refresh_token)
            def parsedResponse = new JsonSlurper().parseText(connectionResponse)
            if(parsedResponse?.Errors){
                errorService?.logError("Error removing d2l user ${d2l_id} from course ${org_unit_id} in brightspaceService.removeUserFromCourse. Errors ${JsonOutput.toJson(parsedResponse?.Errors)}")
            }else{
                removeUserFromCourseDB(d2l_id, org_unit_id)
            }
            return parsedResponse
        }
    }

    def removeUserFromCourseDB(d2l_id,org_unit_id){
        def deleteStatement = """delete from beardata.brightspace_enrollments where parent_code = :course and child_code = :mnum"""
        def mnum = getMnumByD2LID(d2l_id)
        def course = getCourseCodeByOrgUnitId(org_unit_id)
        return sqlService?.execute(deleteStatement,[mnum:mnum,course:course])?:null
    }

    def getBrightspaceCommunityRoles(){
        def query = """select substr(CUSTOM_LOOKUP_VALUE_DESC,1,20) ROLE, custom_lookup_value role_id
                        from beardata.mis_custom_lookup_values
                        inner join beardata.mis_custom_lookup_types on CUSTOM_LOOKUP_TYPE_ID = MIS_CUSTOM_LOOKUP_TYPE_ID 
                            and custom_lookup_type = 'Brightspace Roles'
                        where custom_lookup_attribute2 = 'Y'"""//is community role
        return sqlService?.rows(query)?:null
    }

    def getBrightspaceAcademicRoles(){
        def query = """select substr(CUSTOM_LOOKUP_VALUE_DESC,1,20) ROLE, custom_lookup_value role_id
                        from beardata.mis_custom_lookup_values
                        inner join beardata.mis_custom_lookup_types on CUSTOM_LOOKUP_TYPE_ID = MIS_CUSTOM_LOOKUP_TYPE_ID 
                            and custom_lookup_type = 'Brightspace Roles'
                        where custom_lookup_attribute1 = 'Y'"""//is academic role
        return sqlService?.rows(query)?:null
    }

    def getCommunitiesForUser(userMnum){
        def query = """select code, name, bco.org_unit_id org_unit_id 
                            from beardata.brightspace_enrollments enrollments
                                inner join beardata.bsp_course_offering bco on bco.code = enrollments.parent_code
                                inner join beardata.bsp_user_courses buc on bco.org_unit_id = buc.org_unit_id
                            where enrollments.role_name in ('Instructor','Enrollment Manager')
                                and enrollments.child_code = :mnum
                                and buc.course_type = 'Community'"""
        return sqlService?.rows(query,[mnum:userMnum])?:null
    }


    def checkUsersExist(mnums){
        def query = """select d2l_id, org_defined_id, first_name, last_name, username from beardata.bsp_users where org_defined_id in (${mnums})"""

        return sqlService?.rows(query)

    }

    def enrollUsersInCourse(d2l_ids, course, role){
        def authTokens = getAuthToken()
        if(authTokens){
            def jsonList = []
            d2l_ids?.each {
                def temp = [
                        "OrgUnitId": course,
                        "UserId": it,
                        "RoleId": role
                ]
                jsonList.add(temp)
            }
            String payloadJSON = JsonOutput.toJson(jsonList)

            String apiUrl = '/d2l/api/lp/1.45/enrollments/batch/'
            def uri = new URI(D2L_URL+apiUrl)
            HttpClient client = HttpClient.newHttpClient()
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJSON))
                    .header("Content-Type","application/json")
                    .header("Authorization","Bearer ${authTokens.access_token}")
                    .build()
            String connectionResponse = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
            replaceRefreshToken(authTokens?.refresh_token)
            def parsedResponse
            if(connectionResponse){
                parsedResponse = new JsonSlurper().parseText(connectionResponse)
                //TODO:adapt these to be appropriate for the batch
                if(parsedResponse?.Errors){
                    errorService?.logError("Error bulk adding users to course ${course} in brightspaceService.addUserToCourse. Errors ${JsonOutput.toJson(parsedResponse?.Errors)}")
                }else{
                   addUserToCourseDB(course,user,role)
                }
            }
            def temp = parsedResponse?:null
            //the api call returns a list of failed enrollments, if there are none, it returns an empty list
            //However, if there is an error with the api call, it returns null
            //so because java treats an empty list as falsey, we need to handle that as its own thing
            if(parsedResponse == []){
                return parsedResponse
            }else{
                return parsedResponse?:null
            }

        }else{
            errorService?.logError("Error getting auth tokens in brightspaceService.addUserToCourse")
            return null
        }
    }
}