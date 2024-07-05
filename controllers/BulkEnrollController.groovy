package edu.missouristate.university.brightspace

import org.springframework.security.access.annotation.Secured
import groovy.json.JsonSlurper

@Secured(['IS_AUTHENTICATED_FULLY'])
class BulkEnrollController {

    def pageService, brightspaceService, samlService, personService

    def index() { redirect(action:"checkUsersBulk")}

    def checkUsersBulk(){
        def pvOverrides = [pageTitle: "Bulk Enroll Users in Brightspace Course",proxyRole:"ROLE_BRIGHTSPACE_PROXY",navigation:"/brightspace/navigation"]
        def page = pageService.buildPage(pvOverrides)


        if(params?.d2l_ids&&params?.community&&params?.role){
            def jsonSlurper = new JsonSlurper()
            def d2l_ids = jsonSlurper?.parseText(params?.d2l_ids)
            int role_id = Integer?.parseInt(params?.role?.toString())
            def community_id = Integer?.parseInt(params?.community?.toString())
            def addUsers = brightspaceService?.enrollUsersInCourse(d2l_ids,community_id, role_id)
            if (addUsers?.size() == 0) {
                flash.alert = [type:'success',message:"Users added to course"]
            }else if(addUsers?.size() > 0){
                println "addUsers: ${addUsers}"
                flash.alert = [type:'warning',message:"Some users not added to course"]
            }else{
                flash.alert = [type:'error',message:"Failed to add users to course"]
            }
        }

        [pv:page.pv]
    }

    def validateUsers(){
        def pvOverrides = [pageTitle: "Bulk Enroll Users in Brightspace Course",hideNav:true,proxyRole:"ROLE_BRIGHTSPACE_PROXY"]
        def mnums
        def validUsers = [], invalidUsers = []
        if (params?.mnums) {
            def jsonSlurper = new JsonSlurper()
            mnums = jsonSlurper.parseText(params?.mnums)

            def mnums_clean = params?.mnums?.replaceAll('"',"'")?.replaceAll(/\[|\]/,'')?.toUpperCase()
            validUsers = brightspaceService?.checkUsersExist(mnums_clean)

            invalidUsers = mnums?.findAll{val->
                !validUsers.find{it?.ORG_DEFINED_ID?.toUpperCase()==val?.toUpperCase()}
            }
        }

        def profPidm = samlService?.getPidmProxy(pvOverrides)
        def profMnum = personService?.getBearPassNumberByPidm(profPidm)

        def communities = brightspaceService?.getCommunitiesForUser(profMnum)

        def roles = brightspaceService?.getBrightspaceCommunityRoles()

        render(template: "userValidation",model:[validUsers:validUsers,invalidUsers:invalidUsers,roles:roles,communities:communities])
    }
}