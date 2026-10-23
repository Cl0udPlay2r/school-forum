<script setup>
import {ref} from "vue";
import router from "@/router"
import LightCard from "@/components/LightCard.vue";
import {ElMessage} from "element-plus";
import TopicTag from "@/components/TopicTag.vue";
import {apiForumCollectList, apiForumTopicInteract} from "@/net/api/forum.js";

defineProps({
    show: Boolean
})

const emit = defineEmits(['close'])

const list = ref([])

function init(){
    apiForumCollectList(data => list.value = data)
}

function deleteCollect(index,tid){
  apiForumTopicInteract(tid,'collect','false',() => {
    ElMessage.success('已取消收藏！')
    list.value.splice(index,1);
  })
}
</script>

<template>
    <el-drawer :model-value="show" @close="emit('close')" @open="init" title="我的帖子列表">
        <div class="collect-list">
            <light-card v-for="(item,index) in list" class="collect-card"
                        @click="router.push(`/index/topic-detail/${item.id}`)">
                <topic-tag :type="item.type"/>
                <div class="title">
                    {{item.title}}
                </div>
                <el-link type="danger" @click.stop="deleteCollect(index,item.id)">删除</el-link>
            </light-card>
        </div>
    </el-drawer>
</template>

<style scoped>
.collect-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.collect-card {
    display: flex;
    background-color: rgba(128, 128, 128, 0.2);
    justify-content: space-between;

    .title {
        flex: 1;
        margin-left: 5px;
        font-size: 14px;
        font-weight: bold;
        transition: .3s;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        display: block;

        &:hover {
            cursor: pointer;
            opacity: 0.6;
        }
    }
}
</style>