package cn.hephaestus.smartmeetingroom.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class UserInfo {
    @Null
    private  Integer id;
    @NotNull
    private Boolean sex;
    @Length(max = 11,min = 11,message = "手机号长度必须是11位")
    private String phoneNum;
    @Email
    private String email;

    private String imagePath;
    //人脸数据id
    private Integer fid;
    //组织id
    private Integer oid;
    //部门id
    private Integer did;

    private String name;

    private String nickName;
}
