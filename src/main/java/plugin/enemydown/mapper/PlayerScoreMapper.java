package plugin.enemydown.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import plugin.enemydown.mapper.data.PlayerScore;

public interface PlayerScoreMapper {

  @Select("SELECT * FROM player_score")
  List<PlayerScore> selectList();

  @Insert("insert player_score(player_name, score, difficulty, registered_at) values (#{playerName}, #{score}, #{difficulty}, now())")
  void insert(PlayerScore playerScore);
}
