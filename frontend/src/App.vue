<template>
  <div id="app">
    <!-- regvane -->
    <div v-if="!isLoggedIn && !isLogin" class="auth-container">
      <h2>Регистрация</h2>
      <input v-model="registerUsername" placeholder="Username"/>
      <input v-model="registerPassword" type="password" placeholder="Password" @input="validatePassword"/>
      <div class="password-requirements">
        <span :class="{ valid: passwordLength }">✓ Минимум 8 букви</span>
        <span :class="{ valid: hasUppercase }">✓ 1 Главна буква</span>
        <span :class="{ valid: hasNumber }">✓ 1 число</span>
      </div>
      <h3>Избери аватар</h3>
      <div class="avatar-selection">
        <img
            v-for="(avatar, index) in avatars"
            :key="index"
            :src="avatar"
            :alt="'Avatar ' + index"
            :class="['avatar', { selected: selectedAvatar === avatar }]"
            @click="selectAvatar(avatar)"
        />
      </div>
      <button @click="register">Регистрация</button>
      <div class="register-link">
        Вече имаш профил? <a href="#" @click="switchToLogin">Вход</a>
      </div>
    </div>

    <!-- logvane -->
    <div v-if="!isLoggedIn && isLogin" class="auth-container">
      <h2>Вход</h2>
      <input v-model="loginUsername" placeholder="Username"/>
      <input v-model="loginPassword" type="password" placeholder="Password"/>
      <button @click="login">Влизане</button>
      <p>
        Нямаш акаунт?
        <a href="#" @click="switchToRegister">Регистрирай се тук</a>
      </p>
    </div>

    <!-- chat-a -->
    <div v-if="isLoggedIn" class="chat-container">
      <!-- nov header -->
      <div class="header-container">
        <h1>Welcome to Hidden Chat, {{ username }}!</h1>
        <button @click="logout" class="logout-btn">Изход</button>
      </div>

      <div class="messages">
        <div
            v-for="(message, index) in messageHistory"
            :key="index"
            :class="['message', message.username === username ? 'sent' : 'received']"
        >
          <img v-if="message.avatar" :src="message.avatar" alt="User avatar" class="message-avatar"/>
          <div class="message-content">
            <strong>{{ message.username }}:</strong> {{ message.content }}
            <span class="timestamp">{{ message.timestamp }}</span>
          </div>
        </div>
      </div>

      <div class="input-container">
        <input v-model="newMessage" class="message-input" placeholder="Type a message..."/>
        <button @click="sendMessage">Изпрати</button>
      </div>
    </div>
  </div>
</template>

<script>
import SockJS from "sockjs-client";
import {Client} from "@stomp/stompjs";

export default {
  data() {
    return {
      registerUsername: "",
      registerPassword: "",
      loginUsername: "",
      loginPassword: "",
      selectedAvatar: "",
      avatars: [
        "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Adrian",
        "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Luis",
        "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Brian",
        "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Andrea",
        "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Jameson"
      ],
      username: localStorage.getItem("username") || "",
      isLoggedIn: false,
      isLogin: false,
      passwordLength: false,
      hasUppercase: false,
      hasNumber: false,
      newMessage: "",
      messageHistory: []
    };
  },
  methods: {
    selectAvatar(avatar) {
      this.selectedAvatar = avatar;
    },
    validatePassword() {
      this.passwordLength = this.registerPassword.length >= 8;
      this.hasUppercase = /[A-Z]/.test(this.registerPassword);
      this.hasNumber = /\d/.test(this.registerPassword);
    },
    switchToLogin() {
      this.isLogin = true;
    },
    switchToRegister() {
      this.isLogin = false;
    },

    async register() {
      if (
          this.registerUsername &&
          this.registerPassword &&
          this.selectedAvatar &&
          this.passwordLength &&
          this.hasUppercase &&
          this.hasNumber
      ) {
        const user = {
          username: this.registerUsername,
          password: this.registerPassword,
          avatar: this.selectedAvatar
        };
        const response = await fetch("http://localhost:8080/api/users/register", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(user)
        });
        const result = await response.text();
        alert(result);
        if (result.includes("successfully")) {
          this.username = this.registerUsername;
          this.isLoggedIn = true;
          localStorage.setItem("username", this.username);
          localStorage.setItem("avatar", this.selectedAvatar);
          this.connect();
        }
      } else {
        alert("Паролата е като жената... винаги има изисквания!");
      }
    },
    async login() {
      if (this.loginUsername && this.loginPassword) {
        const user = {
          username: this.loginUsername,
          password: this.loginPassword
        };

        const response = await fetch("http://localhost:8080/api/users/login", {
          method: "POST",
          headers: {"Content-Type": "application/json"},
          body: JSON.stringify(user)
        });

        // polzva se json-a
        const result = await response.json();

        if (response.ok) {
          console.log("Успешно влизане!", result.message);
          this.username = this.loginUsername;
          this.isLoggedIn = true;
          localStorage.setItem("username", this.username);

          // zapisva avatara
          if (result.avatar) {
            this.selectedAvatar = result.avatar;
            localStorage.setItem("avatar", this.selectedAvatar);
          }

          this.connect();
        } else {
          console.log("Грешка при влизане!", result.message);
          alert(result.message);
        }
      } else {
        alert("Попълни всички полета!");
      }
    },
    connect() {
      const socket = new SockJS("http://localhost:8080/chat-websocket");
      // @ts-ignore
      this.stompClient = new Client({
        webSocketFactory: () => socket,
        debug: (str) => console.log(str),
        onConnect: () => {
          console.log("Свързан към WebSocket!");
          this.stompClient.subscribe("/topic/messages", (messageOutput) => {
            console.log("Получено ново съобщение:", messageOutput.body);
            this.messageHistory.push(JSON.parse(messageOutput.body));
          });
        },
        onStompError: (frame) => {
          console.error("STOMP грешка:", frame);
        }
      });
      this.stompClient.activate();
    },
    sendMessage() {
      if (!this.stompClient || !this.stompClient.connected) {
        console.error("WebSocket не е свързан!");
        return;
      }

      if (this.username && this.newMessage) {
        const messageObject = {
          username: this.username,
          content: this.newMessage,
          avatar: localStorage.getItem("avatar"),
          timestamp: new Date().toLocaleTimeString()
        };

        this.stompClient.publish({
          destination: "/app/send",
          body: JSON.stringify(messageObject)
        });

        this.newMessage = "";
      }
    },
    logout() {
      this.username = "";
      this.isLoggedIn = false;
      localStorage.removeItem("username");
      localStorage.removeItem("avatar");
      alert("Успешно излизане!");
    }
  },
  mounted() {
    if (localStorage.getItem("username")) {
      this.username = localStorage.getItem("username");
      this.selectedAvatar = localStorage.getItem("avatar");
    }
  }
};
</script>

<style>
/* suppress warnings */
.sent, .received, .selected {
}

#app {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #eaeaea;


}

body, html {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.auth-container {
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  background: white;
  width: 450px;
  height: 500px;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.auth-container input,
.auth-container button {
  display: block;
  width: 100%;
  max-width: 300px;
  margin: 10px 0;
}

input {
  width: 100%;
  max-width: 300px;
  padding: 10px;
  margin: 10px 0;
  border-radius: 5px;
  border: 1px solid #ccc;
}

.message-input {
  width: 100%;
  max-width: 700px;
  padding: 10px;
  font-size: 16px;
  border-radius: 10px;
  border: 1px solid #ccc;
}


.password-requirements span {
  display: block;
  color: red;
  font-size: 15px;
  margin: 15px 0;
}

.password-requirements .valid {
  color: green;
}

.avatar-selection {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin: 15px 0;
}

.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: transform 0.3s ease;
}

.avatar:hover {
  transform: scale(1.3);
  border-color: #007bff;
}

.avatar.selected {
  border-color: #007bff;
}

button {
  padding: 10px 20px;
  border: none;
  background: #007bff;
  color: white;
  cursor: pointer;
  border-radius: 5px;
  margin-top: 15px;
}

button:hover {
  background: #0056b3;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 80vh;
  width: 80vw;
  max-width: 800px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
}

.message {
  display: flex;
  align-items: center;
  max-width: 60%;
  padding: 10px;
  border-radius: 15px;
  margin: 5px 0;
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sent {
  background-color: #007bff;
  color: white;
  align-self: flex-end;
  text-align: right;
}

.received {
  background-color: #ccc;
  color: black;
  align-self: flex-start;
  text-align: left;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
}

.input-container {
  display: flex;
  width: 100%;
  padding: 10px;
  border-top: 1px solid #ddd;
}

.timestamp {
  display: block;
  font-size: 12px;
  color: white;
  margin-top: 5px;
}

.header-container {
  display: flex;
  justify-content: space-between; /* butona taka e vdqsno ot zaglavieto */
  align-items: center;
  padding: 10px 20px;
  background-color: white;
  border-bottom: 1px solid #ddd;
}

.logout-btn {
  background-color: red;
  color: white;
  border: none;
  padding: 5px 10px;
  font-size: 14px;
  cursor: pointer;
  border-radius: 5px;
}

.register-link a {
  text-decoration: none;
  color: blue;
  font-weight: bold;
  cursor: pointer;
}

.register-link a:hover {
  text-decoration: underline;
}

.register-link {
  margin-top: 20px;
  display: block;
  text-align: center;
}
</style>