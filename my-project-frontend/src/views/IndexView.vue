<script setup>
import {logout} from '@/net'
import router from "@/router";
import {inject, reactive, ref} from "vue";
import {
  Bell,
  Box,
  Brush,
  ChatDotRound,
  Collection,
  DataLine,
  Document,
  Files,
  List,
  Location,
  Lock,
  Monitor,
  Position,
  Reading,
  School,
  Search,
  Setting,
  User
} from "@element-plus/icons-vue";
import {START_LOCATION_NORMALIZED as $route} from "vue-router/dist/devtools-BLCumUwL.mjs";
import LightCard from "@/components/LightCard.vue";
import UserInfo from "@/components/UserInfo.vue";
import {apiUserDeleteNotification, apiUserLoadNotification} from "@/net/api/user.js";
import AIChatWindow from "@/components/AIChatWindow.vue";

const loading = inject("userLoading");

const searchInput = reactive({
  type: '1',
  text: ''
})

const notification = ref([])

const loadNotification = () =>
    apiUserLoadNotification( data => notification.value = data)
loadNotification()

function userLogout() {
  logout(() => router.push("/"))
}

function deleteNotification(url, id) {
  router.push(url)
  apiUserDeleteNotification(id, () => {
    loadNotification()
  })
}

const indexMenu = [
  {
    title: '校园论坛', icon: Location, sub: [
      {title: '帖子广场', icon: ChatDotRound, index: '/index'},
      {title: '失物招领', icon: Box},
      {title: '校园活动', icon: List},
      {title: '表白墙', icon: Brush},
      {title: '合作机构', icon: School}
    ]
  }, {
    title: '探索与发现', icon: Position, sub: [
      {title: '成绩查询', icon: Document},
      {title: '班级课程', icon: Files},
      {title: '教务通知', icon: Monitor},
      {title: '在线图书馆', icon: Collection},
      {title: '预约教室', icon: DataLine}
    ]
  }, {
    title: '个人设置', icon: Setting, sub: [
      {title: '个人信息', icon: User, index: '/index/user-setting'},
      {title: '帖子管理', icon: Reading, index: '/index/topic-setting'},
      {title: '帐号信息', icon: Lock, index: '/index/privacy-setting'}
    ]
  }
]
</script>

<template>
  <div class="main-content" v-loading="loading" element-loading-text="正在进入，请稍后...">
    <el-container style="height: 100%" v-if="!loading">
      <a-i-chat-window/>
      <el-header class="main-content-header">
        <el-image class="logo" src="https://element-plus.org/images/element-plus-logo.svg"/>
        <div style="flex: 1;padding: 0 20px;text-align: center">
          <el-input style="width: 100%;max-width: 500px" v-model="searchInput.text"
                    placeholder="搜索论坛相关内容...">
            <template #prefix>
              <el-icon>
                <Search/>
              </el-icon>
            </template>
            <template #append>
              <el-select style="width: 120px" v-model="searchInput.type">
                <el-option value="1" label="帖子广场"/>
                <el-option value="2" label="校园活动"/>
                <el-option value="3" label="表白墙"/>
                <el-option value="4" label="教务通知"/>
              </el-select>
            </template>
          </el-input>
        </div>
        <user-info>
          <el-popover placement="bottom" :width="350" trigger="click">
            <template #reference>
              <el-badge is-dot  :hidden="!notification.length">
                <div class="notification">
                  <el-icon>
                    <Bell/>
                  </el-icon>
                  <div style="font-size: 14px">消息</div>
                </div>
              </el-badge>
            </template>
            <el-empty :image-size="80" description="暂时没有未读消息" v-if="!notification.length"/>
            <el-scrollbar :max-height="500" v-else>
              <light-card v-for="item in notification" class="notification-item"
                          @click="deleteNotification(item.url,item.id)">
                <div>
                  <el-tag style="translate: 0 -1px" :color="item.type">消息</el-tag>&nbsp;
                  <span>{{ item.title }}</span>
                </div>
              </light-card>
            </el-scrollbar>
          </el-popover>
        </user-info>
      </el-header>
      <el-container>
        <el-aside width="200px">
          <el-scrollbar style="height: calc(100vh - 55px)">
            <el-menu
                router
                :default-active="$route.path"
                :default-openeds="['1','2','3']"
                style="min-height: calc(100vh - 55px)"
            >
              <el-sub-menu :index="(index + 1).toString()"
                           v-for="(menu,index) in indexMenu">
                <template #title>
                  <el-icon>
                    <component :is="menu.icon"/>
                  </el-icon>
                  <span><b>{{ menu.title }}</b></span>
                </template>
                <el-menu-item :index="subMenu.index" v-for="subMenu in menu.sub">
                  <template #title>
                    <el-icon>
                      <component :is="subMenu.icon"></component>
                    </el-icon>
                    <span>{{ subMenu.title }}</span>
                  </template>
                </el-menu-item>
              </el-sub-menu>
            </el-menu>
          </el-scrollbar>
        </el-aside>
        <el-main class="main-content-page">
          <el-scrollbar style="height: calc(100vh - 55px)">
            <router-view v-slot="{Component}">
              <transition name="el-fade-in-linear" mode="out-in">
                <component :is="Component" style="height: 100%"/>
              </transition>
            </router-view>
          </el-scrollbar>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>


<style lang="less" scoped>
.notification-item {
  margin-top: 5px;
  transition: color .3s;

  &:hover {
    cursor: pointer;
    color: gray;
  }
}

.notification {
  font-size: 22px;
  line-height: 14px;
  text-align: center;
  transition: color .3s;

  &:hover {
    color: gray;
    cursor: pointer;
  }
}

.main-content {
  height: 100vh;
  width: 100vw;
}

.main-content-page {
  padding: 0;
  background-color: #f7f8fa;
}

.dark .main-content-page {
  background-color: #26282e;
}

.main-content-header {
  border-bottom: solid 1px var(--el-border-color);
  height: 55px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.logo {
  height: 30px;
}

.user-info {
  display: flex;
  justify-content: flex-end;
  align-items: center;

  .el-avatar:hover {
    cursor: pointer;
  }

  .profile {
    text-align: right;
    margin-right: 20px;

    :first-child {
      font-size: 18px;
      font-weight: bold;
      line-height: 20px;
    }

    :last-child {
      font-size: 11px;
      color: gray;
    }
  }
}
</style>
