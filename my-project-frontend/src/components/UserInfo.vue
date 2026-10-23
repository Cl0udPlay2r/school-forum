<script setup>

import {Back, Right, Setting} from "@element-plus/icons-vue";
import {useStore} from "@/store/index.js";
import {get, isRoleAdmin, logout} from "@/net/index.js";
import router from "@/router/index.js";
import {computed, ref} from "vue";
import {useRoute} from "vue-router";

const store = useStore()
const loading = ref(true);
const route = useRoute()

const isAdminPage = computed(() => route.fullPath.startsWith("/admin"));

get('/api/user/info', (data) => {
  store.user = data
  loading.value = false
})

function userLogout() {
  logout(() => router.push("/"))
}

</script>

<template>
<div class="user-info" v-loading="loading">
  <template v-if="isRoleAdmin()">
    <el-button type="primary" size="small"
               v-if="isAdminPage"
               @click="router.push('/index')">
      去客户端
      <el-icon>
        <Right></Right>
      </el-icon>
    </el-button>
    <el-button type="danger" size="small" v-else
    @click="router.push('/admin')">
      去管理端
      <el-icon>
        <Right></Right>
      </el-icon>
    </el-button>
  </template>

  <slot/>
  <div class="profile">
    <div>{{ store.user.username }}</div>
    <div>{{ store.user.email }}</div>
  </div>
  <el-dropdown>
    <el-avatar :src="store.avatarUrl"/>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item>
          <el-icon>
            <Setting/>
          </el-icon>
          个人设置
        </el-dropdown-item>
        <el-dropdown-item @click="userLogout" divided>
          <el-icon>
            <Back/>
          </el-icon>
          退出登录
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</div>
</template>

<style scoped>
.user-info {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 15px;
  width:350px;

  .el-avatar:hover {
    cursor: pointer;
  }

  .profile {
    text-align: right;


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