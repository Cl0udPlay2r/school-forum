import {get, post} from "@/net/index.js";

export const apiForumType = (success) => {
  get('/api/forum/types', success)
}

//读取帖子详细信息
export const apiForumInit = (tid, success) => {
  get(`/api/forum/topic?tid=${tid}`, success)
}

//获取并改变帖子的某个状态（如点赞，收藏
export const apiForumTopicInteract = (tid, type, status, success) => {
  get(`/api/forum/interact?tid=${tid}&type=${type}&status=${status}`, success)
}

//我的收藏列表
export const apiForumCollectList = (success) => {
  get('/api/forum/collects', success)
}

//更新帖子
export const apiForumUpdateTopic = (tid, editor, success) => {
  post('/api/forum/update-topic', {
    id: tid,
    title: editor.title,
    content: editor.text,
    type: editor.type.id
  }, success)
}

//创建帖子
export const apiForumCreareTopic = (data, success) => {
  post('/api/forum/create-topic', data, success)
}

//加载帖子评论
export const apiForumLoadComments = (tid, page, success) => {
  get(`/api/forum/comments?tid=${tid}&page=${page}`, success)
}

//发表评论
export const apiForumSubmitComment = (data, success) => {
  post('/api/forum/add-comment', data, success)
}

//删除帖子评论
export const apiForumDeleteComment = (id, success) => {
  get(`/api/forum/delete-comment?id=${id}`, success)
}

//读取帖子预览列表
export const apiForumTopTopic = (success) => {
  get('/api/forum/top-topic', success)
}

//更新帖子列表
export const apiForumUpdateTopicList = (topics) => {
  get(`/api/forum/list-topic?page=${topics.page}&type=${topics.type}`, data => {
    if (data) {
      data.forEach(item => {
        topics.list.push(item)
      })
      topics.page++
    }
    if (!data || data.length < 10) topics.end = true
  })
}

//获取管理端的帖子列表信息
export const apiAllForumTopics = (page, size, keyword, success) => {
  get(`/api/admin/forum/list?page=${page}&size=${size}&keyword=${keyword}`, success)
}

//删除帖子(管理员
export const apiForumDelteTopic = (id, success) =>
    get(`/api/admin/forum/delete?topicId=${id}`, success)


//置顶帖子(管理员
export const apiForumSetTop = (data, success) =>
    post('api/admin/forum/top', data, success)


//锁定帖子
export const apiForumLockTopic = (data, success) =>
    post('api/admin/forum/lock', data, success)

//屏蔽帖子
export const apiForumBlockTopic = (data, success) =>
    post('api/admin/forum/invisible', data, success)

//获取违禁词列表
export const apiForumGetProhibitedWords = (success) => {
  get('api/admin/forum/prohibited-list', success)
}

//保存并更新违禁词
export const apiForumSaveProhibitedWords = (data, success) =>
    post('api/admin/forum/prohibited-save', data, success)


//获取用户帖子列表
export const apiForumGetTopics = (success) => {
  get(`/api/forum/self-topics`, success)
}

//删除帖子
export const apiForumDeleteTopic = (tid, success) => {
  get(`/api/forum/delete-topic?tid=${tid}`, success)
}