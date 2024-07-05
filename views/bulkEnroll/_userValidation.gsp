
<br/>
<label for="validUsers" >Successfully found brightspace accounts for these users.</label>

<div class="scrollable-div">
    <table id="validUsers" style="background-color: rgba(120, 142, 30, .1); " >
        <thead>
            <tr>
                <th>
                    M-Number
                </th>
                <th>
                    Name
                </th>
                <th>
                    Username
                </th>
            </tr>
        </thead>
        <tbody>
            <g:each in="${validUsers}">
                <tr>
                    <td>
                        ${it?.ORG_DEFINED_ID}
                    </td>
                    <td>
                        ${it?.FIRST_NAME} ${it?.LAST_NAME}
                    </td>
                    <td>
                        ${it?.USERNAME}
                    </td>
                </tr>
            </g:each>
        </tbody>
    </table>
</div>
<br/>
<label for="invalidUsers" >
    We could not find Brightspace accounts for these users.
</label>

<div class="scrollable-div">
    <table id="invalidUsers" style="background-color: rgba(216, 62, 62, .1);">
        <tr>
            <th>
                M-numbers
            </th>
        </tr>

        <g:each in="${invalidUsers}">
            <tr>
                <td>${it?.toUpperCase()}</td>
            </tr>
        </g:each>

    </table>
</div>
<br/>
<g:form controller="bulkEnroll" action="checkUsersBulk" onsubmit="return validate(this);">
    <input type="hidden" value="${validUsers?.D2L_ID}" name="d2l_ids"/>
    <label for="community" style="width:40%;">Community:</label>
    <select id="community" name="community">
        <g:each in="${communities}">
            <option value="${it?.org_unit_id}" >${it?.name} [${it?.code}]</option>
        </g:each>
    </select>
    <br/>
    <label for="role" style="width:40%;">Role:</label>
    <select id="role" name="role" >
        <g:each in="${roles}">
            <g:if test="${it?.role == 'Participant'}">
                <option value="${it?.role_id}" selected="selected">${it?.role}</option>
            </g:if>
            <g:else>
                <option value="${it?.role_id}">${it?.role}</option>
            </g:else>
        </g:each>
    </select>
    <br/>
    <br/>
    <input type="submit" value="Add users to community"/>
</g:form>