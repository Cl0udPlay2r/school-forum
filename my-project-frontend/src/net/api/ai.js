//ai聊天接口
import {fetchPost} from "@/net/index.js";

// export const apiAiChat = async (context, onMessage, onError, onComplete) => {
//   try {
//     const response = await fetchPost('/api/ai/chat',context)
//
//     const reader = response.body.getReader()
//     const decoder = new TextDecoder();
//
//     while (true) {
//       const {done, value} = await reader.read();
//       if (done) break
//       onMessage(decoder.decode(value))
//     }
//     onComplete()
//   } catch (err) {
//     onError(err)
//   }
// }

export const apiAiChat = async (context, onMessage, onError, onComplete) => {
  try {
    const response = await fetchPost('/api/ai/chat', context);

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let eventDataLines = [];   // 存储当前事件的所有 data 行内容

    while (true) {
      const {done, value} = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, {stream: true});
      const lines = buffer.split('\n');
      buffer = lines.pop() || ''; // 保留可能不完整的最后一行

      for (const line of lines) {
        // 空行 → 表示当前事件结束
        if (line.trim() === '') {
          if (eventDataLines.length > 0) {
            // 用换行连接所有 data 行，还原原始多行文本
            const fullMessage = eventDataLines.join('\n');
            onMessage(fullMessage);
            eventDataLines = []; // 清空，准备下一个事件
          }
          continue;
        }

        // 以 "data:" 开头的行，提取内容（保留原样，不去空格）
        if (line.startsWith('data:')) {
          // 截取 "data:" 之后的所有字符（通常包括一个前导空格，保留）
          const content = line.slice(5);
          // 注意：如果服务端发送 "data: "（有空格但无内容），content 为 " "（单个空格）
          // 如果发送 "data:"（无内容），content 为 ""（空字符串）
          // 无论哪种，都原样存入
          eventDataLines.push(content);
        }
        // 如果还有其他字段（如 event, id），根据需求忽略或处理
      }
    }

    // 处理流结束时 buffer 中可能剩余的数据（最后一个事件可能没有结束空行）
    if (buffer.trim() !== '') {
      // 按相同逻辑拆行处理，但通常不会走到这里
      const lines = buffer.split('\n');
      for (const line of lines) {
        if (line.trim() === '') {
          if (eventDataLines.length > 0) {
            onMessage(eventDataLines.join('\n'));
            eventDataLines = [];
          }
          continue;
        }
        if (line.startsWith('data:')) {
          eventDataLines.push(line.slice(5));
        }
      }
      // 最后如果还有未触发的，触发一次
      if (eventDataLines.length > 0) {
        onMessage(eventDataLines.join('\n'));
      }
    }

    onComplete();
  } catch (err) {
    onError(err);
  }
};