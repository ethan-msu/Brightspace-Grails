package edu.missouristate.university.brightspace

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.springframework.security.access.annotation.Secured


@Secured(['IS_AUTHENTICATED_FULLY'])
class BrightspaceController {

    def pageService, brightspaceService, errorService, samlService, personService, misService

    def index() {
        redirect(action:'addUserPanel')
    }

    def addUserPanel(){
        def pvOverrides = [siteTitle:"Brightspace Add Users ", pageTitle: "Add User To Brightspace Course",navigation:"navigation",proxyRole:"ROLE_BRIGHTSPACE_PROXY"]
        def page = pageService.buildPage(pvOverrides)
        def roles = [132:'Teaching Assistant',133:'Grader',134:'Course Builder']
        def profPidm = samlService?.getPidmProxy(pvOverrides)
        def term = misService?.getCurrentTerm()?.toString()
        //term conversion to our user-friendly format
        if(term.substring(4,6)=='20'){
            term = "SP${term.substring(2,4)}"
        }else if(term.substring(4,6)=='30'){
            term = "SU${term.substring(2,4)}"
        }else if(term.substring(4,6)=='40'){
            term = "FA${term.substring(2,4)}"
        }
        def terms = brightspaceService?.getNearTerms()
        //get courses for prof
        def courses = brightspaceService?.getBspCoursesByInstructor(profPidm,term?.toString())



        [pv:page.pv,courses:courses,roles:roles,profPidm:profPidm,term:term,terms:terms]
    }

    def changeTerm(){
        def profPidm = params?.profPidm
        def term = params?.term
        def courses = brightspaceService?.getBspCoursesByInstructor(profPidm,term)

        render(template:'courses',model:[courses:courses])
    }

    def addUser(){
        def mnum = params?.mnum
        def d2l_id = brightspaceService?.getD2LID(mnum)
        def pidm = personService?.getSpridenByBearPassNumber(mnum)?.SPRIDEN_PIDM
        def term = params?.term
        def isWP = params?.isWP?true:false
        if(pidm&&d2l_id){
            if(params?.course){
                //you can probably do this regex in a slicker way, or just store these values as a json instead...
                def crn = (params?.course=~/(-?\d*):(\d*)/)[0][1]
                def orgUnitId = (params?.course=~/(-?\d*):(\d*)/)[0][2]
                if(crn){
                    //check if student is in class
                    if(isWP){
                        if(!brightspaceService?.isStudentInCourseWP(pidm,crn,term)){
                            brightspaceService.addUserToCourse(Integer?.parseInt(orgUnitId?.toString()),d2l_id,Integer?.parseInt(params?.role?.toString()))
                            flash.alert = [message:"Successfully added user.",type:'success']
                        }else{
                            flash.alert = [message:"User enrolled as student in course and cannot be enrolled as any other role.",type:'error']
                        }
                    }else{
                        if(!brightspaceService?.isStudentInCourse(pidm,crn,term)){
                            brightspaceService.addUserToCourse(Integer?.parseInt(orgUnitId?.toString()),d2l_id,Integer?.parseInt(params?.role?.toString()))
                            flash.alert = [message:"Successfully added user.",type:'success']
                        }else{
                            flash.alert = [message:"User enrolled as student in course and cannot be enrolled as any other role.",type:'error']
                        }
                    }
                }else{
                    flash.alert = [message:"No crn for course found.",type:'error']
                }
            }else{
                flash.alert = [message:"No course info found.",type:'error']
            }
        }else{
            flash.alert = [message:"No user found.",type:'error']
        }
        redirect(action:'addUserPanel',model:params)

    }

    def removeUserPanel(){
        def pvOverrides = [pageTitle: "Remove User From Brightspace Course",navigation:"navigation",proxyRole:"ROLE_BRIGHTSPACE_PROXY"]
        def page = pageService.buildPage(pvOverrides)

        /**
         * This upper section is just to handle course selection. its a bit of a mess.
         */

        def profPidm = samlService?.getPidmProxy(pvOverrides)
        def term = params?.term?:misService?.getCurrentTerm()?.toString()
        //term conversion to our user-friendly format
        if(term.substring(4,6)=='20'){
            term = "SP${term.substring(2,4)}"
        }else if(term.substring(4,6)=='30'){
            term = "SU${term.substring(2,4)}"
        }else if(term.substring(4,6)=='40'){
            term = "FA${term.substring(2,4)}"
        }
        def terms = brightspaceService?.getNearTerms()
        //get courses for prof
        def courses = brightspaceService?.getBspCoursesByInstructor(profPidm,term?.toString())

        /**
         * This section is the important part. We need a list of (non-student/non-instructor) users in a course
         */
        def course = params?.course//org_unit_id
        if (!(course?.toString()?.toBigDecimal() in courses.nonacademicCourses?.ORG_UNIT_ID) && !(course?.toString()?.toBigDecimal() in courses.nonacademicCourses?.ORG_UNIT_ID)){
            course = null//don't let people look at someone else's class list
        }
        def course_name = brightspaceService?.getCourseNameByOrgUnitId(course)
        def users = brightspaceService?.getUsersForCourse(course)

        [pv:page.pv,course:course,users:users,terms:terms,term:term?.toString(),courses:courses,profPidm:profPidm,course_name:course_name]
    }

    def removeUser(){
        try{
            def d2l_id = brightspaceService?.getD2LID(params?.user)
            brightspaceService?.removeUserFromCourse(d2l_id,params?.course)
            flash.alert = [type:"success", message:"Successfully removed user."]
        }catch(Exception e){
            errorService.logError("Error occurred while removing user from brightspace course: ${e.message}")
            flash.alert = [type:"error", message:"Error occurred while removing user."]
        }
        redirect([action:"removeUserPanel",model:params])
    }

    def changeTermSelect(){
        def profPidm = params?.profPidm
        def term = params?.term
        def courses = brightspaceService?.getBspCoursesByInstructor(profPidm,term)

        render(template:'courseSelect',model:[courses:courses])
    }


}