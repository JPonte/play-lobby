const scripts = document.getElementsByTagName('script');
const lastScript = scripts[scripts.length-1];
const url = lastScript.getAttribute('data-url');

const socket = new WebSocket(url);


const inputField = document.getElementById('lobby-message-input');
const chatTextArea = document.getElementById('lobby-chat-textarea');

inputField.onkeydown = (event) => {
    if (event.key === 'Enter') {
        socket.send(inputField.value);
        inputField.value = ''
    }
}

socket.onmessage = (event) => {
    chatTextArea.value += "\n" + event.data;
}