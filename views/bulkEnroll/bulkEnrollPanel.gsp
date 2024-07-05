<%--
  Created by IntelliJ IDEA.
  User: el2316
  Date: 3/5/2024
  Time: 9:42 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <asset:stylesheet src="require-jquery-ui.css"/>
    <asset:javascript src="require-jquery-ui.js"/>
    <meta name="layout" content="mis-2015">
</head>

<body>


<select>
    <g:each in="${communities}">
        <option value="${it?.org_unit_id}" >${it?.name} [${it?.code}]</option>
    </g:each>
</select>


<textarea name="mnums" maxlength="100000" id="mnums"></textarea>




</body>
</html>