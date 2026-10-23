<script setup>
import {
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
  Message,
  Monitor,
  Position,
  School,
  Setting,
  User
} from "@element-plus/icons-vue";
import {inject, onMounted, reactive, ref} from "vue";
import UserInfo from "@/components/UserInfo.vue";
import router from "@/router/index.js";
import {useRoute} from "vue-router";


const route = useRoute()
const searchInput = reactive({
  type: '1',
  text: ''
})

const adminMenu = [
  {
    title: '校园论坛管理', icon: Location, sub: [
      {title: '用户管理', icon: User, index: '/admin/user'},
      {title: '邮件管理', icon: Message, index: '/admin/email'},
      {title: '帖子广场管理', icon: ChatDotRound, index: '/admin/forum'},
      {title: '失物招领管理', icon: Box},
      {title: '校园活动管理', icon: List},
      {title: '表白墙管理', icon: Brush},
      {title: '合作机构管理', icon: School}
    ]
  }, {
    title: '探索与发现管理', icon: Position, sub: [
      {title: '成绩查询管理', icon: Document},
      {title: '班级课程管理', icon: Files},
      {title: '教务通知管理', icon: Monitor},
      {title: '在线图书馆管理', icon: Collection},
      {title: '预约教室管理', icon: DataLine}
    ]
  }, {
    title: '个人设置', icon: Setting, sub: [
      {title: '个人信息管理', icon: User, index: '/index/user-setting'},
      {title: '帐号信息管理', icon: Lock, index: '/index/privacy-setting'}
    ]
  }
]

const loading = inject('userLoading')

const tabsList = ref([])

function handleTabClick({props}){
  router.push(props.name)
}

function closeTab(name){
  const index = tabsList.value.findIndex(tab => tab.name === name)
  const isCurrent = (name === route.fullPath)
  tabsList.value.splice(index, 1)
  if(tabsList.value.length > 0) {
    if(isCurrent){
      router.push(tabsList.value[Math.max(0, index - 1)].name)
    }
  }else{
    router.push('/admin')
  }
}

function addTabs(menu) {
  if (!menu.index) return
  if (tabsList.value.findIndex(tab => tab.name === menu.index) < 0) {
    tabsList.value.push({
          title: menu.title,//tag名称
          name: menu.index//url
        })
  }
}

onMounted(() => {
  const initPage = adminMenu
      .flatMap(menu => menu.sub)
      .find(sub => sub.index === route.fullPath)
  if(initPage){
    addTabs(initPage)
  }
})
</script>

<template>
  <div class="admin-container" v-loading="loading" element-loading-text="正在进入，请稍后...">
    <el-container style="height: 100%">
      <el-aside width="200px">
        <div>
          <el-image class="logo" src="https://element-plus.org/images/element-plus-logo.svg"/>
        </div>
        <el-scrollbar style="height: calc(100vh - 55px)">
          <el-menu
              router
              :default-active="$route.path"
              :default-openeds="['1','2','3']"
              style="min-height: calc(100vh - 55px)"
          >
            <el-sub-menu :index="(index + 1).toString()"
                         v-for="(menu,index) in adminMenu">
              <template #title>
                <el-icon>
                  <component :is="menu.icon"></component>
                </el-icon>
                <span><b>{{ menu.title }}</b></span>
              </template>
              <el-menu-item :index="subMenu.index" @click="addTabs(subMenu)"
                            v-for="subMenu in menu.sub">
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
      <el-container>
        <el-header class="admin-container-header">
          <div style="flex: 1">
            <el-tabs type="card"
                     closable
                     :model-value="route.fullPath"
                     @tab-remove="closeTab"
                     @tab-click="handleTabClick">
              <el-tab-pane v-for="tab in tabsList"
                           :label="tab.title"
                           :name="tab.name"
                           :key="tab.name"/>
            </el-tabs>
          </div>
          <user-info/>
        </el-header>
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

<style scoped>
.admin-container {
  height: 100vh;
  width: 100vw;

  .logo {
    margin: 10px 0 0 10px;
    height: 30px;
  }

  .admin-container-header {
    border-bottom: solid 1px var(--el-border-color);
    height: 55px;
    display: flex;
    align-items: center;
    box-sizing: border-box;

  }

}

.main-content-page {
  padding: 0;
  background-color: #f7f8fa;
}

.dark .main-content-page {
  background-color: #26282e;
}
</style>