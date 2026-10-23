package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Account;
import com.example.entity.dto.StoreImage;
import com.example.mapper.AccountMapper;
import com.example.mapper.StoreImageMapper;
import com.example.service.ImageService;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import io.minio.*;
import io.minio.errors.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class ImageServiceImpl extends ServiceImpl<StoreImageMapper,StoreImage> implements ImageService{
    @Resource
    MinioClient client;

    @Resource
    AccountMapper mapper;

    @Resource
    FlowUtils flowUtils;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
    /**
     * 上传用户头像
     * @param file 头像图片文件
     * @param id 头像要存储的用户id
     * @return 存储在minio的地址
     */

    @Override
    public String uploadAvatar(MultipartFile file, int id) throws IOException {
        String imageName = UUID.randomUUID().toString().replace("-","");
        imageName = "/avatar/" + imageName;
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("study")
                .stream(file.getInputStream(), file.getSize(),-1)
                .object(imageName)
                .build();
        try{
            String avatar = mapper.selectById(id).getAvatar();
            this.deleteOldAvatar(avatar);
            client.putObject(args);
            if(mapper.update(null, Wrappers.<Account>update()
                    .eq("id",id).set("avatar",imageName)) > 0){
                return imageName;
            }else {
                return null;
            }
        }catch (Exception e){
            log.error("图片上传出错："+ e.getMessage(),e);
            return null;
        }
    }

    /**
     * 上传帖子中的图片
     * @param file 图片文件
     * @param id 上传该图片的用户id
     * @return 图片在Minio中的存储路径
     */

    @Override
    public String uploadImage(MultipartFile file, int id) throws IOException {
        String key = Const.FORUM_IMAGE_COUNTER + id;
        if(!flowUtils.limitPeriodCounterCheck(key,20,3600))
            return null;
        String imageName = UUID.randomUUID().toString().replace("-","");
        Date date = new Date();
        imageName = "/cache/" +  format.format(date) + "/" + imageName;
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("study")
                .stream(file.getInputStream(), file.getSize(),-1)
                .object(imageName)
                .build();
        try{
            client.putObject(args);
            if(this.save(new StoreImage(id,imageName,date))){
                return imageName;
            }else {
                return null;
            }
        }catch (Exception e){
            log.error("图片上传出错："+ e.getMessage(),e);
            return null;
        }
    }

    /**
     * 在Minion中查找图片
     * @param stream 图片输出流
     * @param image 图片在minio桶里的名字（路径）
     */

    @Override
    public void fetchImageFromMinio(OutputStream stream, String image)throws Exception {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket("study")
                .object(image)
                .build();
        GetObjectResponse response = client.getObject(args);
        IOUtils.copy(response,stream);
    }

    private void deleteOldAvatar(String avatar) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if(avatar == null || avatar.isEmpty())return;
        RemoveObjectArgs args = RemoveObjectArgs.builder()
                .bucket("study")
                .object(avatar)
                .build();
        client.removeObject(args);
    }
}
