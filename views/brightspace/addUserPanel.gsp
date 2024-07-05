<%--
  Created by IntelliJ IDEA.
  User: el2316
  Date: 1/4/2024
  Time: 7:19 AM
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
    <p style="font-size:larger;color:red;">
        If you have combined courses in the last 24 hours, please ensure you are adding users to the course you merged into.
    </p>

    <g:formRemote url="[controller:'brightspace',action:'changeTerm']" update="coursesTable" name="updateTermForm">
        <input type="hidden" value="${profPidm}" name="profPidm"/>
        <label for="termSelect" style="width:40%;">Term:</label>
        <select name="term" id="termSelect"  style="width:60%;" onchange="updateTerm(this.value);this.parentNode.requestSubmit();" >
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



    <g:form url="[controller:'brightspace',action:'addUser']" name="addUserForm">
        <input type="hidden" value="" name="course" id="course"/>
        <input type="hidden" value="${term}" name="term" id="term"/>


        <label for="isWP">West Plains?</label>
        <input type="checkbox" id="isWP" name="isWP"/>
        <br/>
        <label for="mnum" style="width:40%;">User M-Number:</label>
        <input type="text" pattern="M\d{8}" name="mnum" id="mnum" placeholder="M03234200..."  style="width:40%;">

        <br/>
        <label for="role" style="width:40%;">Role:</label>
        <g:select name="role" from="${roles}" style="width:60%;" optionKey="key" optionValue="value"/>

        <br/>
        <input type="submit" value="Add user to course" name="submit" id="submit" disabled/>

    </g:form>
    <div id="serverResults">

    </div>
    <br/>
    <br/>
    <div id="coursesTable">
        <g:render template="courses" model="${[courses:courses]}"/>
    </div>


    <script>
        let updateCourse = (row)=>{
            document.querySelectorAll('.toggle').forEach((elmnt)=>{
                elmnt.classList.remove('toggle');
            });
            row.classList.add('toggle');
            document.querySelector('#course').value = row.querySelector('input').value;
            document.querySelector('#submit').disabled = false;
        }

        let updateTerm = (term)=>{
            document.querySelector('#term').value = term;
            document.querySelector('#submit').disabled = true;
        }
    </script>
</body>
</html>