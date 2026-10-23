<script setup>
import {EditPen, Search, Unlock, User} from "@element-plus/icons-vue";
import {apiUserAccountList, apiUserModifyPassword} from "@/net/api/user"
import {reactive, ref, watchEffect} from "vue";
import {useStore} from "@/store/index.js";
import UserEditor from "@/components/UserEditor.vue";
import {ElMessage, ElMessageBox} from "element-plus";

const store = useStore()
const userEditorRf = ref()

const userTable = reactive({
  page: 1,
  size: 10,
  total: 0,
  data: []
})

const searchText = ref('')
const keyword = ref('')

function userStatus(row) {
  if (row.mute && row.banned) {
    return '禁言中、封禁中'
  } else if (row.mute) {
    return '禁言中'
  } else if (row.banned) {
    return '封禁中'
  } else {
    return '正常'
  }
}

function changePassword({id, username}) {
  ElMessageBox.prompt(`确定要修改用户${username}的密码吗？`, '修改密码', {
    inputPattern: /^.{6,20}$/,
    inputErrorMessage: '密码位数必须在6~20位之间',
    callback: ({action, value}) => {
      if (action === 'confirm') {
        apiUserModifyPassword({id, newPassword: value}, () => {
          ElMessage.success("密码修改成功")
        })
      }
    }
  })
}

watchEffect(() => {
  apiUserAccountList(userTable.page, userTable.size, keyword.value, data => {
    userTable.total = data.total
    userTable.data = data.list
  })
})

</script>

<template>
  <div class="user-admin">
    <div class="user-admin-header">
      <div>
        <div class="title">
          <el-icon>
            <User/>
          </el-icon>
          论坛用户管理
        </div>
        <div class="desc">
          在这里管理所有的用户，包括帐号信息、封禁和禁言
        </div>
      </div>
      <div style="margin-right: 20px">
        <el-input placeholder="搜索用户id或者相关名称..." :prefix-icon="Search"
                  clearable @clear="keyword=''"
                  @keydown.enter="keyword = searchText "
                  v-model="searchText"/>
      </div>
    </div>

    <el-table height="320" :data="userTable.data">
      <el-table-column prop="id" label="编号" width="80" align="center"/>
      <el-table-column label="用户名" width="180">
        <template #default="{row}">
          <div class="table-username">
            <el-avatar :size="20" :src="store.avatarUserUrl(row.avatar)"/>
            <div>{{ row.username }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="角色">
        <template #default="{row}">
          <el-tag v-if="row.role === 'admin'" type="danger" size="small">管理员</el-tag>
          <el-tag v-else type="success">普通用户</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="电子邮件" width="200" show-overflow-tooltip/>
      <el-table-column label="注册时间" width="200" show-overflow-tooltip>
        <template #default="{row}">
          {{ new Date(row.registerTime).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center">
        <template #default="{row}">
          {{ userStatus(row) }}
        </template>
      </el-table-column>
      <el-table-column label="编辑" width="250" fixed="right" align="center">
        <template #default="{row}">
          <el-button :icon="Unlock" type="warning" size="small"
                     @click="changePassword(row)" :disabled="row.role === 'admin'">修改密码
          </el-button>
          <el-button :icon="EditPen" type="primary" size="small"
                     @click="userEditorRf.openEditPen(row)" :disabled="row.role === 'admin'">编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination :total="userTable.total"
                     v-model:current-page="userTable.page"
                     v-model:page-size="userTable.size"
                     layout="total, sizes ,prev, pager, next, jumper"/>
    </div>

    <user-editor ref="userEditorRf" :user-table="userTable"/>
  </div>
</template>

<style scoped>
.user-admin {
  .user-admin-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .title {
    font-weight: bold;
    margin: 20px 0 15px 20px;
  }

  .desc {
    margin: 20px 0 10px 25px;
    color: #bababa;
    font-size: 13px;
  }

  .table-username {
    height: 30px;
    display: flex;
    align-items: center;
    gap: 15px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: right;
  }
}
</style>