<%--
    Created by IntelliJ IDEA.
    User: el2316
    Date: 2/8/2024
    Time: 10:42 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <asset:stylesheet src="jquery-ui/1.12.1/require-jquery-ui.css"/>
    <asset:javascript src="jquery-ui/1.12.1/require-jquery-ui.js"/>
    <meta name="layout" content="mis-2015">
    <style>
    .toggle{
        background-color: #5e0009;
        color:white;
    }
    </style>
</head>

<body>

<g:formRemote url="[controller:'brightspace',action:'changeTermSelect']" update="courseSelect" name="updateTermForm">
    <input type="hidden" value="${profPidm}" name="profPidm"/>
    <label for="termSelect" style="width:40%;">Term:</label>
    <select name="term" id="termSelect"  style="width:60%;" onchange="this.parentNode.requestSubmit();" >
        <g:each in="${terms}">
            <g:if test="${term == it}">
                <option value="${it}" selected>${it}</option>
            </g:if>
            <g:else>
                <option value="${it}" >${it}</option>
            </g:else>
        </g:each>
    </select>
    <br/>
</g:formRemote>

<div id="courseSelect">
    <g:render template="courseSelect" model="${params}"/>
</div>

<p>Currently viewing users for ${course_name}</p>

<table>
    <tr>
        <th>Name</th>
        <th>M-Number</th>
        <th>Role</th>
    </tr>

    <g:each in="${users}" var="user">
        <tr onclick="updateUserRow(this)">
            <input type="hidden" value="${user?.child_code}"/>
            <td>${user?.NAME}</td>
            <td>${user?.child_code}</td>
            <td>${user?.ROLE}</td>
        </tr>
    </g:each>

</table>

<g:form name="removeUser" controller="brightspace" action="removeUser">
    <input type="hidden" name="course" value="${course}"/>
    <input type="hidden" name="user" id="user" value=""/>
    <input type="submit" name="Remove" id="Remove" value="Remove" disabled/>

</g:form>

<script>
    let updateUserRow = (row)=>{
        document.querySelectorAll('.toggle').forEach((elmnt)=>{
            elmnt.classList.remove('toggle');
        });
        row.classList.add('toggle');
        document.querySelector('#user').value = row.querySelector('input').value;
        document.querySelector('#Remove').disabled = false;
    }
</script>

</body>
</html>