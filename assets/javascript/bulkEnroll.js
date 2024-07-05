function findMNums(inputString) {
    //function to get every "valid" Missouri State University M-Number
    const regexPattern = /(M|m)\d{8}/g;
    const matchingSubstrings = inputString.match(regexPattern);
    return matchingSubstrings;
}

function updateUsersList(textArea){
    let mnumList = findMNums(textArea.value)

    document.querySelector('#mnums').value = JSON.stringify(mnumList)

}


function validate(form){
    let role = form.querySelector('#role').value;
    let communitySelect = form.querySelector('#community');
    let community = communitySelect.options[communitySelect.selectedIndex].innerHTML;

    let a = 'Caution: You have selected to enroll these users as Enrollment Managers. Please ensure your have chosen the correct role before submitting.';

    return (role == '135'&&confirm(a))||confirm(`Add users to ${community}?`)
}