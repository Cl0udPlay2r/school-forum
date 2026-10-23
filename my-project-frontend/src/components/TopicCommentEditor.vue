<script setup lang="ts">
import {Delta, QuillEditor} from "@vueup/vue-quill";
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import {ref} from "vue";
import {ElMessage} from "element-plus";
import {apiForumSubmitComment} from '@/net/api/forum'

const props = defineProps({
    show: Boolean,
  tid: String,
    quote: Object
})

const content = ref()

const emit = defineEmits(['close','comment'])

const init = () => content.value = new Delta()

function submitComment(){
    if (deltaToText(content.value).length > 2000) {
        ElMessage.warning('评论字数过长！')
        return
    }
    apiForumSubmitComment({
        tid: props.tid,
        quote: props.quote ? props.quote.id : -1 ,
        content: JSON.stringify(content.value)
    },() => {
        ElMessage.success('评论发表成功!')
        emit('comment')
    })
}

function  deltaToSimpleText(delta) {
    let str = deltaToText(JSON.parse(delta))
    if(str.length > 35) str = str.substring(0,35) + '...'
    return str
}

function deltaToText(delta) {
    if (!delta?.ops) return ""
    let str = ''
    for (let op of delta.ops) {
        str += op.insert
    }
    //   /\s/g 是特殊字符
    return str.replace(/\s/g, "")
}
</script>

<template>
    <div>
        <el-drawer :model-value="show"
                   :title="quote ? `发表对评论 “${deltaToSimpleText(quote.content)}” 的回复` : '发表帖子评论'"
                   @open="init"
                   @close="emit('close')"
                   :direction="'btt'"
                   :size="280"
                   :close-on-click-modal="false">
            <div>
                <div>
                    <quill-editor style="height: 140px"  v-model:content="content"
                                  placeholder="请友善评论..."/>
                </div>
                <div style="display: flex">
                    <div style="flex: 1;font-size: 13px;color: gray;margin-top: 10px">
                        字数统计:{{deltaToText(content).length}}字 (最长2000字)
                    </div>
                    <el-button style="margin-top: 10px"
                               type="success" plain @click="submitComment">发表评论</el-button>
                </div>
            </div>
        </el-drawer>
    </div>
</template>

<style lang="less" scoped>
:deep(.el-drawer) {
    width: 700px;
    margin: 20px auto;
    border-radius: 10px;
}

:deep(.el-drawer__header) {
    margin: 0;
}
:deep(.el-drawer__body) {
    padding: 5px;
}
</style>