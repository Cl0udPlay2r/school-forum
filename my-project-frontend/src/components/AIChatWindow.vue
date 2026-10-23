<script setup>
import {ref} from "vue";
import {CloseBold} from "@element-plus/icons-vue";
import {apiAiChat} from "@/net/api/ai"
import MarkdownIt from "markdown-it";

const md = new MarkdownIt()

const chatLoading = ref(false);
const isOpen = ref(false)
const inputText = ref('')
const messages = ref([])

const renderMarkdown = (text) => {
  return md.render(text)
}

function sendMessage() {
  if (inputText.value) {
    messages.value.push({
      type: 'user',
      text: inputText.value,
      timestamp: Date.now(),
    })

    inputText.value = ''
    chatLoading.value = true

    messages.value.push({
      type: 'assistant',
      text: "  ",
      timestamp: Date.now(),
    })

    const context = [...messages.value]
    context.length -= 1

    apiAiChat(context, (text) => {
      messages.value[messages.value.length - 1].text += text
    }, () => {
      messages.value[messages.value.length - 1].text = '发送失败，请重试'
      chatLoading.value = false
    }, () => {
      chatLoading.value = false
    })

  }
}

</script>

<template>
  <div class="ai-chat-window">
    <button v-if="!isOpen" @click="isOpen = true" class="chat-button">
      <span>💬</span>
    </button>
    <div v-if="isOpen" class="chat-window">
      <div class="chat-window__header">
        <h3>校园AI助手</h3>
        <el-icon @click="isOpen = false" class="close-btn">
          <CloseBold/>
        </el-icon>
      </div>
      <div class="chat-window__message">
        <div v-for="(message,index) in messages" :key="index"
             :class="[`message-${message.type}`]">
          <div class="message-text" v-if="message.type === 'user'">
            {{ message.text }}
          </div>
          <div class="message-text markdown-body"
               v-if="message.type === 'assistant'"
               v-html="renderMarkdown(message.text)"></div>
        </div>
      </div>
      <div class="chat-window__input">
        <input v-model="inputText" @keydown.enter="sendMessage"
               :disabled="chatLoading"
               type="text" placeholder="请输入要询问的内容..."/>
        <button @click="sendMessage" :disabled="chatLoading" class="send-btn">发送</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-chat-window {
  position: fixed;
  bottom: 20px;
  right: 30px;
  z-index: 99;

  .chat-button {
    width: 45px;
    height: 45px;
    border-radius: 50%;
    border: none;
    background: linear-gradient(135deg, #f1cf1d, 0%, #f38964 100%);
    box-shadow: var(--el-box-shadow-lighter);
    color: white;
    cursor: pointer;
    font-size: 18px;
    transition: transform .3s ease;

    &:hover {
      transform: scale(1.1);
    }
  }

  .chat-window {
    width: 380px;
    height: 500px;
    border-radius: 12px;
    box-shadow: var(--el-box-shadow-lighter);
    background-color: white;
    display: flex;
    flex-direction: column;
    animation: slideUp .3s ease;
    overflow: hidden;

    .chat-window__header {
      color: white;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      background: linear-gradient(135deg, #5f95f8, 10%, #85d2f6 100%);

      .dark & {
        background: linear-gradient(135deg, #92b0cc, 10%, rgb(81, 101, 117) 100%);
      }

      h3 {
        margin: 0;
        font-size: 16px;
      }

      .close-btn {
        background: none;
        border: none;
        cursor: pointer;
        color: white;
        font-size: 20px;
        padding: 0;
        width: 24px;
        height: 24px;
      }
    }

    .chat-window__message {
      flex: 1;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      background: #f5f5f5;
      overflow-y: auto;

      .dark & {
        background: #2c2c2c;
      }

      .message-user, .message-assistant {
        width: fit-content;
        max-width: 80%;

        .message-text {
          background: white;
          border-radius: 13px;
          font-size: 14px;
          padding: 8px 16px;
          word-break: break-word;
          overflow: hidden;

          .dark & {
            background: #454545;
          }
        }

      }

      .message-user {
        align-self: flex-end;
      }

      .message-assistant {
        align-self: flex-start;
      }
    }

    .chat-window__input {
      padding: 16px;
      background: white;
      display: flex;

      gap: 8px;

      .dark & {
        background: #454545;
      }

      .send-btn {
        background: linear-gradient(135deg, #5f95f8, 10%, #85d2f6 100%);
        border: none;
        cursor: pointer;
        color: white;
        font-size: 14px;
        padding: 8px 18px;
        width: 80px;
        border-radius: 14px;
        transition: opacity .3s ease;

        .dark & {
          background: linear-gradient(135deg, #92b0cc, 10%, rgb(81, 101, 117) 100%);
        }

        &:hover {
          opacity: .9;
        }
      }

      input {
        flex: 1;
        border: 1px solid #e0e0e0;
        padding: 10px 14px;
        border-radius: 12px;
        font-size: 13px;
        outline: none;

        &:focus {
          border-color: #5f95f8 #5f95f8 #5f95f8 #5f95f8;
        }
      }
    }
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

<style>
.markdown-body {
  p:first-child {
    margin-top: 0
  }
;

  p:last-child {
    margin-bottom: 0
  }
}
</style>