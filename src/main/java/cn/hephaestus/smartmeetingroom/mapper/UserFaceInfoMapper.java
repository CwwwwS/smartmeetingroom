package cn.hephaestus.smartmeetingroom.mapper;

import cn.hephaestus.smartmeetingroom.model.UserFaceInfo;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserFaceInfoMapper {

    @Insert("insert into user_face_info(feature_data) values(#{userFaceInfo.featureData})")
    @Options(useGeneratedKeys = true,keyProperty = "faceInfoId",keyColumn = "face_info_Id")
    public int addFaceInfo(@Param("userFaceInfo") UserFaceInfo userFaceInfo);

    @Delete("delete from user_face_info where face_info_id = #{faceInfoId}")
    public void deleteFaceInfo(@Param("faceInfoId") Integer faceInfoId);

    @Update("update user_face_info set feature_data = #{userFaceInfo.featureData} where face_info_id = #{userFaceInfo.faceInfoId}")
    public void updateFaceInfo(@Param("userFaceInfo") UserFaceInfo userFaceInfo);

    @Select("select feature_data from user_face_info where face_info_id = #{faceInfoId}")
    public String getFaceInfo(@Param("faceInfoId") Integer faceInfoId);
}
