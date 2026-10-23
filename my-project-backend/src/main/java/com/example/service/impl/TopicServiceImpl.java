package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.*;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.*;
import com.example.mapper.*;
import com.example.service.NotificationService;
import com.example.service.TopicService;
import com.example.utils.CacheUtils;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import com.example.utils.ProhibitedUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    @Resource
    TopicTypeMapper topicTypeMapper;

    @Resource
    FlowUtils flowUtils;

    @Resource
    CacheUtils cacheUtils;

    @Resource
    ProhibitedUtils prohibitedUtils;

    @Resource
    StringRedisTemplate template;

    @Resource
    AccountMapper accountMapper;

    @Resource
    AccountDetailsMapper accountDetailsMapper;

    @Resource
    AccountPrivacyMapper accountPrivacyMapper;

    @Resource
    TopicCommentMapper commentMapper;

    @Resource
    NotificationService notificationService;

    private Set<Integer> types = null;

    @PostConstruct
    private void initTypes() {
        types = this.listTypes()
                .stream()
                .map(TopicType::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 启动补偿：如果在"延迟落库"窗口内进程退出了（崩溃、重启、热部署），
     * Redis 里会残留一批还没写进 MySQL 的互动增量 —— 因为那个延迟任务只活在内存里，
     * 跟着进程一起没了，之后不会再有任何人来刷它。所以启动时补刷一次。
     * 落库用的是 insert ignore + 幂等 delete，即使上次已经刷了一半，重复执行也是安全的；
     * 刷失败时异常会在 template.delete 之前抛出，数据仍然留在 Redis 里，不会丢。
     */
    @EventListener(ApplicationReadyEvent.class)
    void flushLeftoverInteract() {
        // 注意：这里故意不用 @PostConstruct —— 补偿可能要写很多行数据，
        // 放在 Bean 初始化阶段会直接把整个应用启动阻塞住
        // （曾实测因为一条几千条件的 DELETE 把启动拖到 158 秒）。
        // 改成 ApplicationReadyEvent：Tomcat 已经可用，补偿在后台完成即可。
        for (String type : List.of("like", "collect")) {
            // 两个键都要看：type 是还没搬走的，type:flushing 是搬走了但还没落完库的
            boolean pending = template.hasKey(type)
                    || template.hasKey(type + Const.FLUSHING_SUFFIX);
            if (!pending) continue;
            log.warn("检测到上次未落库的[{}]互动数据，正在补偿写入数据库", type);
            try {
                this.saveInteract(type);
            } catch (Exception e) {
                log.error("补偿写入[{}]互动数据失败，数据仍保留在Redis中：{}", type, e.getMessage());
            }
        }
    }

    @Override
    public List<TopicType> listTypes() {
        return topicTypeMapper.selectList(null);
    }

    /**
     * 发表帖子
     *
     * @param uid 用户id
     * @param vo  前端请求的用户编辑的帖子内容
     * @return 不为空返回失败原因 为空是成功
     */
    @Override
    public String createTopic(int uid, TopicCreateVO vo) {
        JSONObject content = vo.getContent();
        if (content == null) return "帖子内容为空！";
        if (!textLimitCheck(content, 20000)) return "帖子内容太多，发文失败！";
        if (!types.contains(vo.getType())) return "文章类型非法！";
        if (!flowUtils.limitPeriodCounterCheck(Const.FORUM_TOPIC_CREATE_COUNTER + uid,
                5, 3600))
            return "发文频繁，请稍后再试!";
        if (prohibitedUtils.containProhibitedWord(vo.getContent()) ||
                prohibitedUtils.containProhibitedWord(vo.getTitle()))
            return "帖子中包含违禁词！请重新编辑后发送";
        Topic topic = new Topic();
        BeanUtils.copyProperties(vo, topic);
        topic.setContent(content.toJSONString());
        topic.setUid(uid);
        topic.setTime(new Date());
        topic.createIntro();
        if (this.save(topic)) {
            //当发帖成功时将所以缓存清空
            cacheUtils.deleteFromCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
            return null;
        } else {
            return "内部错误，请联系管理员";
        }
    }

    @Override
    public String updateTopic(TopicUpdateVO vo, int uid) {
        JSONObject content = vo.getContent();
        if (content == null) return "帖子内容为空！";
        if (!textLimitCheck(content, 20000)) return "帖子内容太多，发文失败！";
        if (!types.contains(vo.getType())) return "文章类型非法！";
        if (!flowUtils.limitPeriodCounterCheck(Const.FORUM_TOPIC_CREATE_COUNTER + uid,
                5, 3600))
            return "修改频繁，请稍后再试!";
        if (prohibitedUtils.containProhibitedWord(vo.getContent()))
            return "帖子中包含违禁词！请重新编辑后发送";
        int result = baseMapper.update(null, Wrappers.<Topic>update()
                .eq("uid", uid)
                .eq("id", vo.getId())
                .eq("locked", false)
                .set("title", vo.getTitle())
                .set("content", content.toString())
                .set("type", vo.getType())
                .set("intro", Topic.recreateIntro(content))
        );
        return result > 0 ? null : "该帖子已被锁定，暂时无法编辑";
    }

    @Override
    public String createComment(AddCommentVO vo, int uid) {
        String content = vo.getContent();
        if (content == null) return "评论内容为空！";
        if (!textLimitCheck(JSONObject.parseObject(content), 2000)) return "评论内容太多，发文失败！";
        if (!flowUtils.limitPeriodCounterCheck(Const.FORUM_TOPIC_COMMENT_COUNTER + uid,
                3, 60))
            return "评论频繁，请稍后再试!";
        if (prohibitedUtils.containProhibitedWord(vo.getContent()))
            return "帖子中包含违禁词！请重新编辑后发送";
        TopicComment comment = new TopicComment();
        comment.setUid(uid);
        BeanUtils.copyProperties(vo, comment);
        comment.setTime(new Date());
        commentMapper.insert(comment);
        //下面是通知功能的实现 有两种通知 一种评论了该用户的帖子的评论 一种是评论了该用户评论的评论
        Topic topic = baseMapper.selectById(vo.getTid());//获取该评论的帖子信息
        Account account = accountMapper.selectById(uid);//获取发表评论的用户信息
        if (vo.getQuote() > 0) {  //quote大于一说明这个发表的评论是引用了其他评论 （下面判断是否为回复自己的评论）
            TopicComment com = commentMapper.selectById(vo.getQuote());
            if (!Objects.equals(account.getId(), com.getUid())) {//评论评论的用户不是自己
                notificationService.addNotification(
                        com.getUid(),
                        "您有新的评论回复"
                        , account.getUsername() + "回复了您的评论，快去看看吧"
                        , "success"
                        , "/index/topic-detail/" + com.getTid()
                );
            }
        } else if (!Objects.equals(account.getId(), topic.getUid())) {//当该评论的用户和帖子用户不相同时
            notificationService.addNotification(
                    topic.getUid(),
                    "您有新的帖子回复"
                    , account.getUsername() + "回复了您的帖子:" + topic.getTitle() + "，快去看看吧"
                    , "success"
                    , "/index/topic-detail/" + topic.getId()
            );
        }
        return null;
    }

    @Override
    public List<CommentVO> listComments(int tid, int pageNumber) {
        Page<TopicComment> page = new Page<>(pageNumber, 10);
        commentMapper.selectPage(page, Wrappers.<TopicComment>query().eq("tid", tid));
        return page.getRecords().stream().map(dto -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(dto, vo);
            if (dto.getQuote() > 0) {
                TopicComment comment = commentMapper.selectOne(Wrappers.<TopicComment>query()
                        .eq("id", dto.getQuote()).orderByAsc("time"));
                if (comment != null) {
                    JSONObject object = JSONObject.parseObject(comment.getContent());
                    StringBuilder builder = new StringBuilder();
                    this.shortContent(object.getJSONArray("ops"), builder, ignore -> {
                    });
                    vo.setQuote(builder.toString());
                } else {
                    vo.setQuote("此评论已被删除！");
                }

            }
            CommentVO.User user = new CommentVO.User();
            Account account = accountMapper.selectById(dto.getUid());
            BeanUtils.copyProperties(account, user);
            user.setName(account.getUsername());
            vo.setUser(user);
            return vo;
        }).toList();
    }

    @Override
    public void deleteComment(int id, int uid) {
        commentMapper.delete(Wrappers.<TopicComment>query().eq("id", id).eq("uid", uid));
    }

    @Override
    public void deleteTopic(int tid) {
        baseMapper.deleteById(tid);
        cacheUtils.deleteFromCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        baseMapper.deleteCollects(tid);
    }

    /**
     * 用户删除自己的帖子
     *
     * @param tid 帖子id
     * @param uid 用户id
     */
    @Override
    public void deleteUserTopic(int tid, int uid) {
        int result = baseMapper.delete(Wrappers.<Topic>query()
                .eq("id", tid)
                .eq("uid", uid)
        );
        if (result > 0) {
            cacheUtils.deleteFromCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
            baseMapper.deleteCollects(tid);
        }

    }

    @Override
    public void setTop(int tid, boolean status) {
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("top", status));
    }

    /***
     * 更新帖子锁定状态
     * @param tid 帖子id
     * @param status 锁定状态
     */

    @Override
    public void lockTopic(int tid, boolean status) {
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("locked", status));
    }

    /***
     * 更新帖子屏蔽状态
     * @param tid 帖子id
     * @param status 屏蔽状态
     */
    @Override
    public void blockTopic(int tid, boolean status) {
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("invisible", status));
    }

    /**
     * 获取用户收藏列表
     *
     * @param uid 用户id
     * @return 收藏列表
     */
    @Override
    public List<TopicPreviewVO> listTopicCollects(int uid) {
        return baseMapper.listCollects(uid)
                .stream()
                .map(topic -> {
                    TopicPreviewVO vo = new TopicPreviewVO();
                    BeanUtils.copyProperties(topic, vo);
                    return vo;
                }).toList();
    }

    /**
     * 获取帖子列表，将帖子转化成帖子预览格式列表返回
     *
     * @param pageNumber 开始页面 默认为0
     * @param type       查看帖子类型 默认为0（全查看）
     * @return 预览格式的帖子
     */
    @Override
    public List<TopicPreviewVO> listTopicByPage(int pageNumber, int type) {
        String key = Const.FORUM_TOPIC_PREVIEW_CACHE + pageNumber + ":" + type;
        List<TopicPreviewVO> list = cacheUtils.takeListFromCache(key, TopicPreviewVO.class);
        if (list != null) return list;
        Page<Topic> page = Page.of(pageNumber, 10);
        if (type == 0)
            baseMapper.selectPage(page, Wrappers.<Topic>query()
                    .eq("invisible", 0)
                    .orderByDesc("time"));
        else
            baseMapper.selectPage(page, Wrappers.<Topic>query()
                    .eq("type", type)
                    .eq("invisible", 0)
                    .orderByDesc("time"));
        List<Topic> topics = page.getRecords();
        if (topics == null) return null;
        list = topics.stream().map(this::resolveToPreview).toList();
        cacheUtils.saveListToCache(key, list, 60);
        return list;
    }

    /**
     *
     * @param page    返回页数
     * @param size    每页大小
     * @param keyword 搜索关键字
     * @return 帖子简略信息列表 || 搜索结果列表
     */
    @Override
    public JSONObject listTopicAdmin(int page, int size, String keyword) {
        Page<Topic> topicPage = baseMapper.selectPage(Page.of(page, size),
                Wrappers.<Topic>query()
                        .select("id", "title", "uid", "type",
                                "time", "top", "locked", "invisible")
                        .like(keyword != null, "title", keyword)
                        .orderByAsc("time"));
        List<TopicPreviewVO> list = topicPage.getRecords().stream().map(this::resolveToPreview).toList();
        JSONObject object = new JSONObject();
        object.put("list", list);
        object.put("total", topicPage.getTotal());
        return object;
    }

    /**
     * 将数据库中获取的topic数据处理成返回给前端的TopicPreviewVO（内容最多展示300字）
     *
     * @param topic 数据库中获取的帖子
     * @return 回给前端的帖子
     */
    private TopicPreviewVO resolveToPreview(Topic topic) {
        TopicPreviewVO preview = new TopicPreviewVO();
        //由于简化了topic表 现在topic只负责自己的数据 不携带account的数据 所以需要另外给preview传一份account的数据
        BeanUtils.copyProperties(accountMapper.selectById(topic.getUid()), preview);
        BeanUtils.copyProperties(topic, preview);
        preview.setLike(baseMapper.interactCount(topic.getId(), "like"));
        preview.setCollect(baseMapper.interactCount(topic.getId(), "collect"));
        List<String> images = new ArrayList<>();
        StringBuilder previewText = new StringBuilder();
        if (topic.getContent() != null) {
            JSONArray ops = JSONObject.parseObject(topic.getContent()).getJSONArray("ops");
            this.shortContent(ops, previewText, obj -> images.add(obj.toString()));
        }
        preview.setImages(images);
        preview.setText(previewText.length() >= 300 ? previewText.substring(0, 300) :
                previewText.toString());
        return preview;
    }

    /**
     * 将一些长文本转化为短文本，超出部分舍去最长为300字
     *
     * @param ops          需要处理的对象数组
     * @param previewText  拼接文本
     * @param imageHandler 处理图片的消费者，一般只有帖子内容需要
     */
    private void shortContent(JSONArray ops, StringBuilder previewText,
                              Consumer<Object> imageHandler) {
        for (Object op : ops) {
            Object insert = JSONObject.from(op).get("insert");
            if (insert instanceof String text) {
                if (previewText.length() >= 300) continue;
                previewText.append(text);
            } else if (insert instanceof Map<?, ?> map) {
                Optional.ofNullable(map.get("image"))
                        .ifPresent(imageHandler);
            }
        }
    }

    @Override
    public List<TopicTopVO> listTopTopics() {
        List<Topic> topics = baseMapper.selectList(Wrappers.<Topic>query()
                .select("id", "title", "time")
                .eq("invisible", false)
                .eq("top", 1));
        return topics.stream().map(topic -> {
            TopicTopVO vo = new TopicTopVO();
            BeanUtils.copyProperties(topic, vo);
            return vo;
        }).toList();
    }

    @Override
    public List<UserTopicPreviewVO> getUserTopic(int uid) {
        List<Topic> list = baseMapper.selectList(Wrappers.<Topic>query().eq("uid", uid));
        return list.stream().map(topic -> {
            UserTopicPreviewVO vo = new UserTopicPreviewVO();
            BeanUtils.copyProperties(topic, vo);
            return vo;
        }).toList();
    }

    /***
     * 获取帖子详细信息
     *
     * @param tid 帖子id
     * @param uid 获取帖子的用户id
     * @return 帖子详细信息 如果帖子被屏蔽则返回空
     */
    @Override
    public TopicDetailVO getTopic(int tid, int uid) {
        TopicDetailVO vo = new TopicDetailVO();
        Topic topic = baseMapper.selectById(tid);
        if (topic.getInvisible() == 1 && !topic.getUid().equals(uid)) return null;
        BeanUtils.copyProperties(topic, vo);
        TopicDetailVO.InteractTopic interact = new TopicDetailVO.InteractTopic(
                hasInteract(tid, uid, "like"),
                hasInteract(tid, uid, "collect")
        );
        vo.setInteractTopic(interact);
        TopicDetailVO.User user = new TopicDetailVO.User();
        vo.setUser(this.fillUserDetailsByPrivacy(user, topic.getUid()));
        vo.setComments(commentMapper.selectCount(Wrappers.<TopicComment>query().eq("tid", tid)));
        return vo;
    }

    /**
     * 保存互动信息
     *
     * @param interact 互动信息
     * @param status   互动状态
     */

    @Override
    public void interact(Interact interact, boolean status) {
        String type = interact.getType();
        // 这里不再需要 synchronized(type.intern())。
        // 原实现把「写 Redis + 调度落库」放在一把按 type 的全局锁里，而锁内包含一次 Redis 往返；
        // 压测中 jstack 抽样 2483 个 HTTP 线程样本，有 63% 处于 BLOCKED（栈顶就是本方法）。
        // 现在写 Redis 不加锁，"读增量 → 落库 → 清空"之间的原子性改由 Redis 的 RENAME 保证，
        // 详见 saveInteract()。
        template.opsForHash().put(type, interact.toKey(), Boolean.toString(status));
        this.saveInteractScheduled(type);
    }

    /**
     * 判断缓存中是否有有关like/collect的数据，如果没有就从数据库中找寻
     * @param tid 帖子id
     * @param uid 用户id
     * @param type 互动类型(like/collect)
     * @return boolean
     */
    private boolean hasInteract(int tid, int uid, String type) {
        //Redis 里缓存的是"还没落库"的互动状态：key 是互动类型（like/collect），field 是 tid:uid
        //注意不要用 entries(type).get(key)：那会把整个 hash 拉回来，这里只需要读一个 field
        Object cached = template.opsForHash().get(type, tid + ":" + uid);
        if (cached != null) {
            return Boolean.parseBoolean(cached.toString());
        }
        //缓存里没有，说明已经落库了（或从未操作过），回查数据库
        return baseMapper.userInteractCount(tid, uid, type) > 0;
    }

    private final Map<String, Boolean> status = new ConcurrentHashMap<>();
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

    /** 落库前先把待处理增量原子地搬到这个后缀的临时键，详见 saveInteract() */
//    private static final String FLUSHING_SUFFIX = ":flushing";

    /**
     * 单条批量 SQL 最多包含多少行，避免语句过大
     */
    private static final int DB_BATCH_SIZE = 500;

    /***
     * 保存帖子状态任务启动管理？ 让其重新写入数据库的操作延迟3秒后再进行，3秒内的其他所有请求都不会对数据库造成影响
     * @param type 收藏|点赞
     */
    private void saveInteractScheduled(String type) {
        // putIfAbsent 本身就是原子的，不再需要额外的锁
        if (status.putIfAbsent(type, Boolean.TRUE) == null) {
            service.schedule(() -> {
                try {
                    this.saveInteract(type);
                } catch (Exception e) {
                    log.error("互动数据落库失败，数据仍保留在 Redis 中，下次交互或重启会重试：{}", e.getMessage());
                } finally {
                    // 必须用 remove，不能 put(type, false)：
                    // putIfAbsent 返回的是「旧值」，留下一个值为 false 的映射后，
                    // 之后的 putIfAbsent 会一直返回 Boolean.FALSE（非 null），
                    // 表现为「从此再也不排任务」——一个 JVM 生命周期里每类互动只会落库第一批，
                    // 其余全部堆在 Redis 里。
                    status.remove(type);
                    // 补一次调度，覆盖两种漏掉的情况：
                    // ① 落库期间（RENAME 之后）又有新的互动写入 type 键；
                    // ② 落库中途失败，这批数据已经搬到 type:flushing 但没落完库。
                    // 不回查的话，这两种数据都要等到"下一次有人交互"才会被刷进数据库。
                    if (template.hasKey(type)
                            || template.hasKey(type + Const.FLUSHING_SUFFIX)) {
                        this.saveInteractScheduled(type);
                    }
                }
            }, 3, TimeUnit.SECONDS);
        }
    }

    /**
     * 把待落库的互动增量写进数据库（全程不需任何 Java 锁）。
     *
     * 关键是第一步：用 Redis 的 RENAME 把待处理数据原子地搬到 "xxx:flushing"。
     * 这一步之后新来的写入会落到新的 hash 里、由下一轮 flush 处理，
     * 不会出现"读完之后、清空之前"的新数据被一起删掉的情况 ——
     * 原来正是为了防这种丢失，才必须用 synchronized(type.intern()) 把两边串行化。
     * 现在原子性交给 Redis，Java 侧无需加锁。
     *
     * 落库本身幂等（insert ignore + 按 (tid,uid) 删除），
     * 因此若在"落库完成但还没删除临时键"之间崩溃，下次启动补偿时重放也是安全的。
     *
     * @param type 收藏|点赞
     */
    private void saveInteract(String type) {
        String drainKey = type + Const.FLUSHING_SUFFIX;
        // 1) 先处理可能残留的临时键（上次崩在删除之前的情况）
        this.flushDrainKey(type, drainKey);
        // 2) 再原子地把当前待处理增量搬走并落库
        if (template.hasKey(type)) {
            template.rename(type, drainKey);
            this.flushDrainKey(type, drainKey);
        }
    }

    /**
     * 把临时键里的数据批量落库，成功后删除它。
     * <p>
     * 分批提交：单条语句过大有两个问题 —— ① 可能超出 max_allowed_packet；
     * ② 一旦某条语句变慢（比如几千个条件的 DELETE），
     * 会把单线程的 Redis / MySQL长时间占住，反过来拖慢所有在线请求。
     */
    private void flushDrainKey(String type, String drainKey) {
        if (!template.hasKey(drainKey)) {
            return;
        }
        List<Interact> check = new LinkedList<>();
        List<Interact> unCheck = new LinkedList<>();
        template.opsForHash().entries(drainKey).forEach((k, v) -> {
            if (Boolean.parseBoolean(v.toString()))
                check.add(Interact.parseInteract(k.toString(), type));
            else
                unCheck.add(Interact.parseInteract(k.toString(), type));
        });
        for (List<Interact> batch : partition(check)) {
            baseMapper.addInteract(batch, type);
        }
        for (List<Interact> batch : partition(unCheck)) {
            baseMapper.deleteInteract(batch, type);
        }
        // 落库成功后才删除：崩在这里的话，下次启动会重放（幂等）
        template.delete(drainKey);
    }

    /**
     * 把列表切成固定大小的小批，避免单条 SQL 过大
     */
    private List<List<Interact>> partition(List<Interact> list) {
        List<List<Interact>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += DB_BATCH_SIZE) {
            result.add(list.subList(i, Math.min(i + DB_BATCH_SIZE, list.size())));
        }
        return result;
    }

    /**
     * 根据用户设置的隐私设置过滤出要展示信息
     *
     * @param target 展示的实体
     * @param uid    用户id
     * @return 过滤完的实体
     */
    private <T> T fillUserDetailsByPrivacy(T target, int uid) {
        Account account = accountMapper.selectById(uid);
        //账号可能已被删除，此时没有任何可填充的信息，直接返回空壳，
        //而不是让整个帖子详情页 500
        if (account == null) return target;
        AccountDetails accountDetails = accountDetailsMapper.selectById(uid);
        AccountPrivacy accountPrivacy = accountPrivacyMapper.selectById(uid);
        //details / privacy 的行是注册时配套插的，历史数据或手工造的账号可能缺失。
        //缺失时分别用「空详情」和「不隐藏任何字段」兜底，而不是抛 NPE 把接口打成 500。
        if (accountDetails == null) accountDetails = new AccountDetails();
        String[] ignores = accountPrivacy == null ? new String[0] : accountPrivacy.hiddenFields();
        BeanUtils.copyProperties(account, target, ignores);
        BeanUtils.copyProperties(accountDetails, target, ignores);
        return target;
    }

    /**
     * 文本长度检查
     * @param object 帖子内容
     * @param max 最大限制字数
     * @return 是否超过限制
     */
    private boolean textLimitCheck(JSONObject object, int max) {
        if (object == null) return false;
        long length = 0;
        for (Object op : object.getJSONArray("ops")) {
            length += JSONObject.from(op).getString("insert").length();
            if (length > max) return false;
        }
        return true;
    }

}
