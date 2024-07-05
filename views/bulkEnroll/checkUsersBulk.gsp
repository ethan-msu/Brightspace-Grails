<%--
  Created by IntelliJ IDEA.
  User: el2316
  Date: 3/6/2024
  Time: 7:38 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <asset:stylesheet src="require-jquery-ui.css"/>
    <asset:javascript src="require-jquery-ui.js"/>
    <asset:javascript src="jquery/1.12.1/require-jquery-only.js"/>
    <meta name="layout" content="mis-2015">
    <asset:javascript src="brightspace/bulkEnroll.js"/>

    <style>
        .scrollable-div {
            overflow-y: scroll; /* Add a vertical scrollbar when content overflows */
            max-height: 400px;
            border: 1px solid #c2c2c2;
        }
        .scrollable-div thead{
            position:sticky;
            top:0;
            z-index:1;

        }
        select{
            width:60%;
        }
    </style>
</head>

<body>
<mis:displaySpinner/>

<p style="font-size:larger;color:red;">
    Standard academic courses cannot be controlled with the bulk enrollment tool.
    This tool can only be used for specialized areas such as communities.
    You will be prompted to select an eligible course after you add the users and select <span style="font-weight:bold">Check Students</span>.
    <br/>
    <br/>
    You may enroll up to 1000 users at a time.
    Users not yet in Brightspace cannot be added using these tools.
</p>

<label for="mnumsText">M-Numbers:</label>
<textarea name="mnumsText" maxlength="100000" id="mnumsText" onchange="updateUsersList(this)" placeholder="Paste a list of M-numbers here..."></textarea>

<g:formRemote update="results" name="validateUsersForm" url="[controller:'bulkEnroll',action:'validateUsers']">
    <input type="hidden" name="mnums" id="mnums" value=""/>
    <input type="submit" value="Check Students"/>
</g:formRemote>

<div id="results">

</div>

</body>
</html>