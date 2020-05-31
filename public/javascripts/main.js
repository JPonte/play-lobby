const scripts = document.getElementsByTagName('script');
const lastScript = scripts[scripts.length-1];
const url = lastScript.getAttribute('data-url');

const socket = new WebSocket(url);


const inputField = document.getElementById('lobby-message-input');
const chatTextArea = document.getElementById('lobby-chat-textarea');
const userList = document.getElementById('lobby-user-list')

inputField.onkeydown = (event) => {
    if (event.key === 'Enter') {
        socket.send(inputField.value);
        inputField.value = ''
    }
}

socket.onmessage = (event) => {
    const jsonData = JSON.parse(event.data)
    console.log(jsonData)
    if ("SystemLobbyMessage" in jsonData) {
        chatTextArea.value += "\n" + jsonData["SystemLobbyMessage"]["content"];
    } else if ("LobbyMessage" in jsonData) {
            chatTextArea.value += "\n" + jsonData["LobbyMessage"]["sender"]["value"] + ": " + jsonData["LobbyMessage"]["content"];
    } else if ("UpdatedUsersList" in jsonData) {
        userList.innerHTML = ''
        jsonData["UpdatedUsersList"]["userList"].forEach(function (user, index) {

            userList.innerHTML += '<li>'+user+'</li>'
            console.log(user)
        })
    }
}