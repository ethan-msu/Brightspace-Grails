
<g:form action="removeUserPanel" controller="brightspace" >
    <label for="courseSelect" style="width:40%;">Course:</label>
    <select name="course" id="courseSelect"  style="width:60%;" >
        <g:each in="${courses?.academicCourses}" >
            <g:if test="${it?.ORG_UNIT_ID?.toString() == course?.toString()}">
                <option value="${it?.ORG_UNIT_ID}" selected>${it?.NAME}-${it?.CRN}</option>
            </g:if>
            <g:else>
                <option value="${it?.ORG_UNIT_ID}">${it?.NAME}-${it?.CRN}</option>
            </g:else>
        </g:each>
        <g:each in="${courses?.nonacademicCourses}">
            <g:if test="${it?.ORG_UNIT_ID?.toString() == course?.toString()}">
                <option value="${it?.ORG_UNIT_ID}" selected>${it?.NAME}</option>
            </g:if>
            <g:else>
                <option value="${it?.ORG_UNIT_ID}">${it?.NAME}</option>
            </g:else>
        </g:each>
    </select>
    <br/>
    <input type="submit" value="Update Selected Course"/>

</g:form>
