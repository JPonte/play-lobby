const scripts = document.getElementsByTagName('script');
const lastScript = scripts[scripts.length-1];
const url = lastScript.getAttribute('data-url');

const socket = new WebSocket(url);


const inputField = document.getElementById('lobby-message-input');
const userList = document.getElementById('lobby-user-list')
const chatArea = document.getElementById('chat-area'); 

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
        const div = document.createElement("div");
        div.innerHTML = "<b>" + jsonData["SystemLobbyMessage"]["content"]+ "</b>";
        chatArea.prepend(div);
    } else if ("LobbyMessage" in jsonData) {
            const div = document.createElement("div");
            div.innerHTML = "<b>" + jsonData["LobbyMessage"]["sender"]["value"] + ":</b> " + jsonData["LobbyMessage"]["content"];
            chatArea.prepend(div)
    } else if ("UpdatedUsersList" in jsonData) {
        userList.innerHTML = ''
        jsonData["UpdatedUsersList"]["userList"].forEach(function (user, index) {
            userList.innerHTML += '<li>'+user+'</li>'
        })
    }
}

let modalBtn = document.getElementById("create-game-button")
let modal = document.querySelector(".modal")
let closeBtn = document.querySelector(".close-btn")
modalBtn.onclick = function () {
    modal.style.display = "block"
}
closeBtn.onclick = function () {
    modal.style.display = "none"
}
window.onclick = function (e) {
    if (e.target == modal) {
        modal.style.display = "none"
    }
}