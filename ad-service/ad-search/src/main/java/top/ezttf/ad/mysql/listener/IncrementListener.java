package top.ezttf.ad.mysql.listener;

import com.github.shyiko.mysql.binlog.event.EventType;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.ezttf.ad.mysql.constant.DBConstant;
import top.ezttf.ad.mysql.binlog.BinlogRowData;
import top.ezttf.ad.mysql.constant.OpType;
import top.ezttf.ad.mysql.dto.MySQLRowData;
import top.ezttf.ad.mysql.dto.TableTemplate;
import top.ezttf.ad.sender.ISender;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author yuwen
 * @date 2019/1/31
 */
@Slf4j
@Component
public class IncrementListener implements IListener {

    private final ISender iSender;
    private final AggregationListener aggregationListener;

    // TODO ISender可能有多个bean实例, 后期指明Qualifier
    @Autowired
    public IncrementListener(
            @Qualifier(value = "sender") ISender iSender,
            AggregationListener aggregationListener) {
        this.iSender = iSender;
        this.aggregationListener = aggregationListener;
    }
    /**
     * 注册需要处理的表
     */
    @Override
    @PostConstruct
    public void register() {
        log.info("IncrementListener register db as table info");
        DBConstant.table2Db.forEach((key, value) -> {
            aggregationListener.register(value, key, this);
        });
    }

    /**
     * 事件处理逻辑, 投放增量数据
     * @param eventData
     */
    @Override
    public void onEvent(BinlogRowData eventData) {
        TableTemplate tableTemplate = eventData.getTableTemplate();
        EventType eventType = eventData.getEventType();

        // 将数据包装为需要投递的数据格式 MySQLRowData
        MySQLRowData mySQLRowData = new MySQLRowData();
        mySQLRowData.setTableName(tableTemplate.getTableName());
        mySQLRowData.setLevel(tableTemplate.getLevel());
        mySQLRowData.setOpType(OpType.to(eventType));

        // 取出模板中操作对应的字段列表
        List<String> fieldList = tableTemplate.getOpTypeFieldSetMap().get(mySQLRowData.getOpType());
        if (fieldList == null) {
            // template.json中表示的表中不存在该操作类型。数据不应该被处理
            log.warn("{} not support for {}", mySQLRowData.getOpType(), mySQLRowData.getTableName());
            return;
        }
        // TODO 测试一下能不能不 new Map 直接传参 afterMap
        eventData.getAfter().forEach(afterMap ->
                mySQLRowData.getFieldValueMapList().add(Maps.newHashMap())
        );
        iSender.sender(mySQLRowData);
    }
}