<script setup>
import {apiForumDeleteTopic, apiForumGetTopics, apiForumType} from '@/net/api/forum'
import {ref} from "vue";
import Card from "@/components/Card.vue";
import {useStore} from "@/store/index.js";
import TopicTag from "@/components/TopicTag.vue";
import {Clock} from "@element-plus/icons-vue";
import {ElMessage, ElMessageBox} from "element-plus";

const topicsList = ref([])
const store = useStore()

function refreshTopicList() {
  apiForumGetTopics((data) => topicsList.value = data)
}

const deleteTopic = (tid) => {
  ElMessageBox.confirm('确定要删除该帖子吗？删除后无法复原，请慎重考虑...', "确认删除", {
    callback: value => {
      if (value === 'confirm') {
        apiForumDeleteTopic(tid, () => {
          ElMessage.success('删除成功!')
          refreshTopicList()
        })
      }
    }
  })
}

refreshTopicList()
apiForumType((data) => store.forum.types = data)
</script>

<template>
  <div class="user-topics">
    <card class="topic-card" v-for="topic in topicsList">
      <div>
        <div class="title">
          <topic-tag :type="topic.type" style="margin-right: 5px"/>
          <el-link :href="`/index/topic-detail/${topic.id}`">
            {{ topic.title }}
          </el-link>
        </div>
        <div class="user-topic-time">
          <el-icon>
            <Clock/>
          </el-icon>
          {{ new Date(topic.time).toLocaleString() }}
        </div>
      </div>
      <div>
        <el-button type="danger" plain size="small" @click="deleteTopic(topic.id)">删除帖子</el-button>
      </div>
    </card>
  </div>
</template>

<style scoped>
.user-topics {
  margin: auto;
  max-width: 700px;

  .topic-card {
    margin-top: 10px;
    display: flex;
    justify-content: space-between;

    .user-topic-time {
      margin-top: 10px;
      font-size: 13px;
      color: grey;
    }
  }
}
</style>