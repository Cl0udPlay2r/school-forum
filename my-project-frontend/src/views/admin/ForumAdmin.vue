<script setup lang="js">
import {reactive, ref, watchEffect} from "vue";
import {
  apiAllForumTopics,
  apiForumBlockTopic,
  apiForumDelteTopic,
  apiForumGetProhibitedWords,
  apiForumLockTopic,
  apiForumSaveProhibitedWords,
  apiForumSetTop,
  apiForumType
} from '@/net/api/forum.js'
import {Hide, Lock, Search, Top, User} from "@element-plus/icons-vue";
import {useStore} from "@/store/index.js";
import ColorDot from "@/components/ColorDot.vue";
import {ElMessage, ElMessageBox} from "element-plus";

const store = useStore()

const types = ref([])
const prohibitedWords = ref('')
const findType = (type) => types.value.find(x => x.id === type)

const searchText = ref('')
const keyword = ref('')

const topicList = reactive({
  list: [],
  page: 1,
  size: 10,
  total: 0
})

//删除帖子
const deleteTopic = (id) => {
  ElMessageBox.confirm(`确定要删除编号为${id}的帖子吗`, '删除帖子', {
    callback: value => {
      if (value === 'confirm') {
        apiForumDelteTopic(id, () => {
          refreshTopic()
          ElMessage.success("删除成功")
        })
      }
    }
  })
}

//更新列表or获取
function refreshTopic() {
  apiAllForumTopics(topicList.page, topicList.size, keyword.value, (data) => {
    topicList.list = data.list
    topicList.total = data.total
  })
}

//置顶帖子
const setTop = (tid, status) => {
  apiForumSetTop({tid, status}, () => {
    refreshTopic()
    ElMessage.success("置顶成功")
  })

}
//锁定
const lockTopic = (tid, status) => {
  apiForumLockTopic({tid, status}, () => {
    refreshTopic()
  })
}
//屏蔽
const blockTopic = (tid, status) => {
  apiForumBlockTopic({tid, status}, () => {
    refreshTopic()
  })
}

//更新违禁词
const saveProhibited = () => {
  const list = prohibitedWords.value.split(',')
  console.log(list)
  apiForumSaveProhibitedWords(list, () => {
    ElMessage.success("违禁词保存成功！")
  })
}

watchEffect(() => {
  refreshTopic()
})

apiForumType((data) => types.value = data)
apiForumGetProhibitedWords((data) => prohibitedWords.value = data.join(','))
</script>

<template>
  <div class="forum-admin">
    <div class="forum-admin-header">
      <div>
        <div class="forum-admin-title">
          <el-icon>
            <User/>
          </el-icon>
          帖子管理
        </div>
        <div class="forum-admin-desc">
          在这里管理所有用户发表的帖子
        </div>
      </div>
      <div style="margin-right: 20px">
        <el-input placeholder="搜索相关帖子标题..." :prefix-icon="Search"
                  clearable @clear="keyword=''"
                  @keydown.enter="keyword = searchText "
                  v-model="searchText"/>
      </div>

    </div>
    <el-table :data="topicList.list" height="450">
      <el-table-column prop="id" label="帖子id" width="150" align="center"/>
      <el-table-column prop="title" label="标题" width="250" show-overflow-tooltip>
        <template #default="{row}">
          <el-link :href="`/index/topic-detail/${row.id}`">
            <div style="display: inline-flex;gap: 5px;color: dodgerblue;margin-right: 5px">
              <el-icon v-if="row.locked">
                <Lock/>
              </el-icon>
              <el-icon v-if="row.top">
                <Top/>
              </el-icon>
              <el-icon v-if="row.invisible">
                <Hide/>
              </el-icon>
            </div>
            {{ row.title }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="帖子类型" width="250">
        <template #default="{row}">
          <div>
            <color-dot :color="`${row.type ? findType(row.type)?.color : 'gray'}`"/>
            <span style="margin-left: 5px">
                    {{ row.type ? findType(row.type)?.name : '未知类型' }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="帖子作者" width="250">
        <template #default="{row}">
          <div class="topic-username">
            <el-avatar :size="20" :src="store.avatarUserUrl(row.avatar)"/>
            <div>{{ row.username }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop='time' label="发送时间"
                       :formatter="row => new Date(row.time).toLocaleString()"/>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{row}">
          <el-button type="info" v-if="row.invisible"
                     @click="blockTopic(row.id,false)" size="small">取消
          </el-button>
          <el-button type="info" plain v-else
                     @click="blockTopic(row.id,true)" size="small">屏蔽
          </el-button>
          <el-button type="warning" v-if="row.locked"
                     @click="lockTopic(row.id,false)" size="small">取消
          </el-button>
          <el-button type="warning" plain size="small"
                     v-else @click="lockTopic(row.id,true)">锁定
          </el-button>
          <el-button type="success"
                     @click="setTop(row.id,false)" v-if="row.top" size="small">取消
          </el-button>
          <el-button type="success" plain
                     @click="setTop(row.id,true)" v-else size="small">置顶
          </el-button>
          <el-button type="danger" plain @click="deleteTopic(row.id)" size="small">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination :total="topicList.total"
                     v-model:current-page="topicList.page"
                     v-model:page-size="topicList.size"
                     layout="total, sizes ,prev, pager, next, jumper"/>
    </div>
    <div class="prohibited">
      <div class="prohibited-title">违禁词管理</div>
      <div class="prohibited-desc">帖子或评论中包含有关屏蔽词则无法发出，每个屏蔽词之间用逗号隔开</div>
      <el-input type="textarea" rows="6" v-model="prohibitedWords"/>
      <div class="prohibited-button">
        <el-button type="primary" @click="saveProhibited">保存违禁词</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forum-admin {
  .forum-admin-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .forum-admin-title {
    font-weight: bold;
    margin: 20px 0 15px 20px;
  }

  .forum-admin-desc {
    margin: 20px 0 10px 25px;
    color: #bababa;
    font-size: 13px;
  }

  .topic-username {
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

  .prohibited {
    padding: 0 15px;

    .prohibited-title {
      font-weight: bold;
    }

    .prohibited-desc {
      margin: 8px 0;
      font-size: 13px;
      color: #bababa;
    }

    .prohibited-button {
      margin-top: 15px;
      text-align: end;
    }
  }
}
</style>