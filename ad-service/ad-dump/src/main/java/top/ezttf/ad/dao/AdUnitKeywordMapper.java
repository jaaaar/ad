package top.ezttf.ad.dao;

import org.apache.ibatis.annotations.Mapper;
import top.ezttf.ad.dump.table.AdUnitKeywordTable;
import top.ezttf.ad.pojo.AdUnitKeyword;

import java.util.List;

@Mapper
public interface AdUnitKeywordMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(AdUnitKeyword record);

    int insertSelective(AdUnitKeyword record);

    AdUnitKeyword selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(AdUnitKeyword record);

    int updateByPrimaryKey(AdUnitKeyword record);




    List<AdUnitKeywordTable> selectAdUnitKeywordTable();
}