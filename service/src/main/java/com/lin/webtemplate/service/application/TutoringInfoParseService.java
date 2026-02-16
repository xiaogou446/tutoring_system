package com.lin.webtemplate.service.application;

import com.lin.webtemplate.infrastructure.entity.ArticleRawEntity;
import com.lin.webtemplate.infrastructure.entity.TutoringInfoEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 功能：家教文本解析服务，将 article_raw 转为结构化检索字段。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Service
public class TutoringInfoParseService {

    private static final String[] DISTRICTS = {
            "浦东", "闵行", "徐汇", "黄浦", "静安", "普陀", "杨浦", "宝山", "嘉定", "松江", "青浦", "奉贤", "金山", "崇明"
    };

    private static final String[] GRADES = {
            "幼儿", "小学", "初中", "高中", "高一", "高二", "高三", "初一", "初二", "初三", "一年级", "二年级", "三年级", "四年级", "五年级", "六年级"
    };

    private static final String[] SUBJECTS = {
            "语文", "数学", "英语", "物理", "化学", "生物", "历史", "地理", "政治", "奥数", "编程", "钢琴", "小提琴"
    };

    private static final Pattern SALARY_PATTERN = Pattern.compile("(\\d{2,5}(?:\\.\\d+)?\\s*(?:元/小时|元每小时|元/次|元/月|元|k|K|千|万))");

    private static final Pattern CONTACT_PATTERN = Pattern.compile("(1\\d{10}|[wW][eE][iI][xX][iI][nN][:\\s-]*[a-zA-Z0-9_-]{4,}|微信[号:：\\s-]*[a-zA-Z0-9_-]{4,})");

    public TutoringInfoEntity parse(ArticleRawEntity rawEntity) {
        TutoringInfoEntity entity = new TutoringInfoEntity();
        entity.setId(rawEntity.getId());
        entity.setSourceUrl(rawEntity.getSourceUrl());
        entity.setTitle(rawEntity.getTitle());
        entity.setPublishTime(rawEntity.getPublishTime());
        entity.setContentText(rawEntity.getContentText());

        String mergedText = normalize(rawEntity.getTitle()) + "\n" + normalize(rawEntity.getContentText());
        entity.setDistrict(firstMatchedKeyword(mergedText, DISTRICTS));
        entity.setGrade(firstMatchedKeyword(mergedText, GRADES));
        entity.setSubject(firstMatchedKeyword(mergedText, SUBJECTS));
        entity.setTeacherGender(parseTeacherGender(mergedText));
        entity.setSalary(parseByPattern(mergedText, SALARY_PATTERN));
        entity.setContact(parseByPattern(mergedText, CONTACT_PATTERN));
        entity.setParsedAt(LocalDateTime.now());
        return entity;
    }

    private String firstMatchedKeyword(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private String parseTeacherGender(String text) {
        if (text.contains("男老师") || text.contains("男教员")) {
            return "男";
        }
        if (text.contains("女老师") || text.contains("女教员")) {
            return "女";
        }
        if (text.contains("不限")) {
            return "不限";
        }
        return null;
    }

    private String parseByPattern(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            if (pattern == SALARY_PATTERN && text.contains("面议")) {
                return "面议";
            }
            return null;
        }
        return matcher.group(1);
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }
}
