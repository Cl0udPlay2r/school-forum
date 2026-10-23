<script setup>

import LightCard from "@/components/LightCard.vue";
import {
  ArrowRightBold,
  CircleCheck,
  Clock,
  CollectionTag,
  EditPen,
  FolderOpened,
  Lock,
  MostlyCloudy,
  Star
} from "@element-plus/icons-vue";
import Weather from "@/components/Weather.vue";
import {computed, onMounted, reactive, ref, watch} from "vue";
import {get} from "@/net/index.js";
import {ElMessage} from "element-plus";
import TopicEditor from "@/components/TopicEditor.vue";
import {useStore} from "@/store/index.js";
import ColorDot from "@/components/ColorDot.vue";
import router from "@/router/index.js";
import TopicTag from "@/components/TopicTag.vue";
import InteractButton from "@/components/InteractButton.vue";
import TopicCollectList from "@/components/TopicCollectList.vue";
import {apiForumTopTopic, apiForumUpdateTopicList} from "@/net/api/forum.js";

const store = useStore();

const editor = ref(false)
const topics = reactive({
    list: [],
    type: 0,
    page: 0,
    end: false,
    top: []
})

const collects = ref(false)

watch(() => topics.type, () => resetList(), {immediate: true})


const today = computed(() => {
    const data = new Date()
    return `${data.getFullYear()} 年 ${data.getMonth() + 1} 月 ${data.getDate()} 日`
})



function updateList() {
    if (topics.end) return
  apiForumUpdateTopicList(topics)
}

function onTopicCreate() {
    editor.value = false
    resetList()
}

function resetList() {
    topics.page = 0
    topics.list = []
    topics.end = false
    updateList()
}

const weather = reactive({
    location: {},
    now: {},
    hourly: [],
    success: false
})

navigator.geolocation.getCurrentPosition(position => {
  const longitude = position.coords.longitude
  const latitude = position.coords.latitude
  get(`/api/forum/weather?longitude=${longitude}&latitude=${latitude}`, data => {
    Object.assign(weather, data)
    weather.success = true
  })
}, error => {
  ElMessage.warning("位置信息获取超时，请检查网络设置")
  get(`/api/forum/weather?longitude=112.41&latitude=34.61`, data => {
    Object.assign(weather, data)
    weather.success = true
  })
}, {
  timeout: 5000,
  enableHighAccuracy: true
})

onMounted(() => {
  apiForumTopTopic(data => topics.top = data)
})
</script>

<template>
    <div style="display: flex;margin: auto;max-width: 900px;padding: 0 20px">
        <div style="flex: 1;margin: 20px;width: 620px">
            <light-card>
                <div class="create-topic" @click="editor=true">
                    <el-icon style="translate: 0 2px;margin-right: 3px ">
                        <EditPen/>
                    </el-icon>
                    点击我发表主题...
                </div>
            </light-card>
            <light-card style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px">
                <div class="top-topic" v-for="item in topics.top" @click="router.push(`/index/topic-detail/${item.id}`)">
                  <el-tag type="primary" size="small">置顶</el-tag>
                    <div>{{item.title}}</div>
                    <div>{{new Date(item.time).toLocaleDateString()}}</div>
                </div>
            </light-card>
            <light-card style="margin-top: 10px;display: flex;gap: 10px">
                <div :class="`type-select-card ${topics.type === item.id ? 'active' : ''}`"
                     v-for="item in store.forum.types"
                     @click="topics.type = item.id">
                    <color-dot :color="item.color"/>
                    <span style="margin-left: 3px">{{ item.name }}</span>
                </div>
            </light-card>
            <transition name="el-fade-in" mode="out-in">
                <div v-if="topics.list.length">
                    <div style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px"
                         v-infinite-scroll="updateList">
                        <light-card v-for="item in topics.list" class="topic-card"
                                    @click="router.push('/index/topic-detail/'+item.id)">
                            <div>
                                <div style="display: flex">
                                    <div>
                                        <el-avatar size="small"
                                                   :src="store.avatarUserUrl(item.avatar)"/>
                                    </div>
                                    <div style="margin-left: 10px;transform: translateY(-4px)">
                                        <div style="font-size: 13px;font-weight: bold">{{ item.username }}</div>
                                        <div style="font-size: 12px;color: gray">
                                            <el-icon>
                                                <Clock/>
                                            </el-icon>
                                            <div
                                                style="display: inline-block;margin-left: 5px;transform: translateY(-2px)">
                                                {{ new Date(item.time).toLocaleString() }}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                              <el-tag type="warning" size="small"
                                      style="margin-right: 4px"
                                      v-if="item.locked">
                                <el-icon>
                                  <Lock/>
                                </el-icon>
                                已锁定
                              </el-tag>
                                <topic-tag :type="item.type" />
                                <span style="font-weight: bold;margin-left: 8px">{{ item.title }}</span>
                            </div>
                            <div class="topic-content">{{ item.text }}</div>
                            <div style="display: grid;grid-template-columns: repeat(3,1fr);gap: 10px">
                                <el-image class="topic-image" v-for="img in item.images" :src="img" fit="cover"/>
                            </div>
                            <div style="display: flex;font-size: 13px;color: gray;margin-top: 10px" >
                                <interact-button>
                                    <el-icon style="translate: 0 2px"><CircleCheck/></el-icon> {{item.like}}点赞
                                </interact-button>
                                <interact-button style="margin-left: 20px">
                                    <el-icon style="translate: 0 2px"><Star/></el-icon>{{item.collect}}收藏
                                </interact-button>
                            </div>
                        </light-card>
                    </div>
                </div>
            </transition>
        </div>
        <div style="width: 280px;margin:20px">
            <light-card>
                <div class="collect-list-button" @click="collects = true">
                    <span><el-icon style="margin-right: 3px;transform: translateY(2px)">
                        <FolderOpened/></el-icon>我的收藏
                    </span>
                    <el-icon style="transform: translateY(4px)"><ArrowRightBold/></el-icon>
                </div>
            </light-card>
            <light-card style="margin-top: 10px">
                <div>
                    <el-icon style="translate: 0 3px;margin-right: 3px ">
                        <CollectionTag/>
                    </el-icon>
                    <span style="font-weight: bold">论坛公告</span>
                </div>
                <el-divider style="margin: 10px"/>
                <div class="notice-text" >
                    墩子会喜欢时田其实很反直觉，因为人潜意识都觉得女神会配帅哥，会更配小山内这种外形有风度的男人，
                    甚至更配粉川警官这个亲手救下她的人。但墩子和时田的追求才是一致的，一样的纯粹，充满趣味的内心
                    。我觉得那句话说得很好，小山内喜欢墩子，粉川警官喜欢红辣椒，但只有时田才关注墩子的整体
                </div>
            </light-card>
            <light-card style="margin-top: 20px">
                <div>
                    <el-icon style="translate: 0 3px;margin-right: 3px ">
                        <MostlyCloudy/>
                    </el-icon>
                    <span style="font-weight: bold">天气信息</span>
                </div>
                <el-divider style="margin: 10px"/>
                <weather :data="weather"/>
            </light-card>
            <light-card style="margin-top: 20px">
                <div>
                    <div class="info-text">
                        <div>当前时间：</div>
                        <div>{{ today }}</div>
                    </div>
                    <div class="info-text">
                        <div>今日运势：</div>
                        <div style="font-weight: bold">大吉</div>
                    </div>
                </div>
            </light-card>
        </div>
        <topic-editor :show="editor" @success="onTopicCreate()" @close="editor=false"/>
        <topic-collect-list :show="collects" @close="collects = false"/>
    </div>
</template>

<style lang="less" scoped>
.collect-list-button {
    font-size: 14px;
    display: flex;
    justify-content: space-between;
    transition: .3s;

    &:hover {
        cursor: pointer;
        opacity: 0.6;
    }
}
.notice-text {
  font-size: 14px;
  color: grey;
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 7;
}

.top-topic{
    display: flex;

    div:first-of-type{
        margin-left: 8px;
        font-size: 14px;
        font-weight: bold;
        opacity: 0.8;
        transition: color .3s;

        &:hover{
            color: grey;
        }
    }
    div:nth-of-type(2){
        flex: 1;
        font-size: 13px;
        color: grey;
        text-align: right;
    }

    &:hover{
        cursor: pointer;
    }
}
.type-select-card {
    background-color: #f5f5f5;
    padding: 3px 5px;
    font-size: 14px;
    box-sizing: border-box;
    border-radius: 4px;
    transition: background-color .3s;

    &:active {
        border: 1px solid #ead4c4;
    }

    &:hover {
        cursor: pointer;
        background-color: #dadada;
    }
}

.topic-card {
    padding: 15px;
    transition: scale .3s;

    &:hover {
        scale: 1.015;
        cursor: pointer;
    }

    .topic-content {
        font-size: 13px;
        color: gray;
        margin: 5px 0;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 3;
        overflow: hidden;
        text-overflow: ellipsis;
    }



    .topic-image {
        width: 100%;
        height: 100%;
        max-height: 110px;
        border-radius: 5px;
    }
}

.info-text {
    display: flex;
    justify-content: space-between;
    color: gray;
}

.create-topic {
    height: 40px;
    background-color: #d5d6da;
    border-radius: 5px;
    line-height: 40px;
    padding: 0 10px;
    color: gray;
    font-size: 14px;

    &:hover {
        cursor: pointer;
    }
}

.dark {
    .create-topic {
        background-color: #2c2c2c;
    }

    .type-select-card {
        background-color: #282828;

        &:active {
            border: 1px solid #64594b;
        }

        &:hover {
            background-color: #5e5e5e;
        }
    }
}
</style>