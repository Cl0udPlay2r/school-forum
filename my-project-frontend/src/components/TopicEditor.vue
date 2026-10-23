<script setup>
import {computed, reactive, ref} from "vue";
import {Document} from "@element-plus/icons-vue";
import {Delta, Quill, QuillEditor} from "@vueup/vue-quill";
import ImageResize from "quill-image-resize-vue";
import {ImageExtend, QuillWatch} from "quill-image-super-solution-module";
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import {accessHeader} from "@/net/index.js";
import {ElMessage} from "element-plus";
import axios from "axios";
import ColorDot from "@/components/ColorDot.vue";
import {useStore} from "@/store/index.js";
import {apiForumCreareTopic} from "@/net/api/forum.js";

const store = useStore();

const props = defineProps({
    show: Boolean,
    defaultTitle: {
        default: '',
        type: String
    },
    defaultText: {
        default: '',
        type: String
    },
    defaultType: {
        default: null,
        type: Number
    },
    submitButton: {
        default: '立即发表主题',
        type: String
    },
    submit:{
        default: (editor,success) => {
            apiForumCreareTopic({
                title: editor.title,
                content: editor.text,
                type: editor.type.id
            }, () => {
                ElMessage.success("帖子发表成功！")
                success();
            })
        },
        type:Function
    }
})

const refEditor = ref()
const emit = defineEmits(['close', 'success'])

const editor = reactive({
    type: null,
    title: '',
    text: '',
    loading: false,
})

function initEditor() {
    if(props.defaultText)
        editor.text = new Delta(JSON.parse(props.defaultText))
    else
        refEditor.value.setContents("", 'user')
    editor.type = store.findTypeById(props.defaultType)
    editor.title = props.defaultTitle

}

function deltaToText(delta) {
    if (!delta.ops) return ""
    let str = ''
    for (let op of delta.ops) {
        str += op.insert
    }
    //   /\s/g 是特殊字符
    return str.replace(/\s/g, "")
}

const contentLength = computed(() => {
    return deltaToText(editor.text).length
})

function submitTopic() {
    var length = deltaToText(editor.text).length;
    if (length > 20000) {
        ElMessage.warning("字数超出限制，无法发表主题！")
        return
    }
    if (editor.title == null || editor.title === "") {
        ElMessage.warning("请填写标题！")
        return
    }
    if (!editor.type) {
        ElMessage.warning("请选择主题类型！")
        return
    }
    props.submit(editor,() => emit('success'))
}


Quill.register('modules/ImageResize', ImageResize)
Quill.register('modules/ImageExtend', ImageExtend)

const editorOption = {
    modules: {
        toolbar: {
            container: [
                "bold", "italic", "underline", "strike", "clean",
                {color: []}, {'background': []},
                {size: ["small", false, "large", "huge"]},
                {header: [1, 2, 3, 4, 5, 6, false]},
                {list: "ordered"}, {list: "bullet"}, {align: []},
                "blockquote", "code-block", "link", "image",
                {indent: '-1'}, {indent: '+1'}
            ],
            headers: {
                'image': function () {
                    QuillWatch.emit(this.quill.id)
                }
            }
        },
        ImageResize: {
            modules: ['Resize', 'DisplaySize']
        },
        ImageExtend: {
            action: axios.defaults.baseURL + '/api/image/cache',
            name: 'file',
            size: 5,
            loading: true,
            accept: 'image/png,image/jpeg',
            response: (resp) => {
                if (resp.data) {
                    return axios.defaults.baseURL + '/images' + resp.data
                } else {
                    return null
                }
            },
            methods: 'POST',
            headers: xhr => {
                xhr.setRequestHeader('Authorization', accessHeader().Authorization)
            },
            start: () => editor.uploading = true,
            success: () => {
                ElMessage.success('图片上传成功!')
                editor.uploading = false
            },
            error: () => {
                ElMessage.warning('图片上传失败，请联系管理员!')
                editor.uploading = false
            }
        }
    }
}
</script>

<template>
    <el-drawer :model-value="show"
               :direction="'btt'"
               @open="initEditor"
               :size="700"
               :close-on-click-modal="false"
               @close="emit('close')">
        <template #header>
            <div>
                <div style="font-weight: bold">发表新的帖子</div>
                <div style="font-size: 13px;color: gray">帖子内容请不要违反相关法律法规，
                    友善发言 不要出现侮辱性词汇。恶语伤人心，善言结良友...
                </div>
            </div>
        </template>
        <div style="display: flex;gap: 10px">
            <div style="width: 150px">
                <el-select placeholder="选择主题类型..." value-key="id" v-model="editor.type" >
                    <el-option v-for="item in store.forum.types.filter( type => type.id > 0)" :value="item" :label="item.name">
                        <div>
                            <color-dot :color="item.color"/>
                            <span style="margin-left: 10px">{{ item.name }}</span>
                        </div>
                    </el-option>
                </el-select>
            </div>
            <div style="flex: 1">
                <el-input style="height: 100%" v-model="editor.title" placeholder="请输入主题标题..."
                          maxlength="30" :perfix-icon="Document"/>
            </div>
        </div>
        <div style="margin-top: 5px;font-size: 13px;color: gray">
            <color-dot :color="`${editor.type ? editor.type.color : 'gray'}`"/>
            <span style="margin-left: 5px">
                    {{ editor.type ? editor.type.desc : '请在上方选择一个帖子类型' }}
                </span>
        </div>
        <div style="margin-top: 10px;margin-bottom: 5px;
                    height: 440px;overflow: unset;border-radius: 5px"
             v-loading=editor.uploading
             element-loading-text="正在上传图片，请稍后...">
            <quill-editor v-model:content="editor.text"
                          content-type="delta"
                          :options="editorOption"
                          ref="refEditor"
                          style="height: calc(100% - 44px)" placeholder="今天要分享点什么呢..."/>
        </div>
        <div style="display: flex;justify-content: space-between;margin-top: 30px">
            <div style="color: gray;font-size: 13px">当前字数:{{ contentLength }}字(最多20000字)</div>
            <el-button type="success" @click="submitTopic" plain>{{submitButton}}</el-button>
        </div>
    </el-drawer>
</template>

<style scoped>
:deep(.el-drawer) {
    width: 700px;
    margin: auto;
    border-radius: 10px 10px 0 0;
}

:deep(.el-drawer__header) {
    margin: 0;
}
</style>