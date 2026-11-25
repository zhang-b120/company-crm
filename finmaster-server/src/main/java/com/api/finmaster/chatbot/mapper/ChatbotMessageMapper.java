package com.qykj.finmaster.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.chatbot.domain.ChatbotMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatbotMessageMapper extends BaseMapper<ChatbotMessage> {
}
