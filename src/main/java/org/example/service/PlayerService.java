package org.example.service;

import lombok.Data;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.example.utils.UserIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.ListUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by daqwang on 28/11/20.
 */
@Data
@Service
public class PlayerService {

    private Logger logger = LoggerFactory.getLogger(PlayerService.class);

    private int currentPlayers;
    private List<StompPrincipal> players;

    public PlayerService() {
        if (players == null) {
            players = new ArrayList<>();
        }
    }


    public List<StompPrincipal> assignRolesForPlayers(List<StompPrincipal> players) {
        if (ListUtils.isEmpty(players) || checkMinimumPlayerNumber(players.size())) {
            logger.error("invalid number of players: {},need at least 7 players.", players.size());
        }
        List<Role> roleList = showRoles(players.size());
        Collections.shuffle(roleList);
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setRole(roleList.get(i));
            roleList.remove(i);
        }
        return players;
    }

    public StompPrincipal createPlayer() {
        String userId = UserIdGenerator.generateUserId();
        StompPrincipal stompPrincipal = new StompPrincipal(userId);
        while (players.contains(stompPrincipal)) {
            stompPrincipal.setName(UserIdGenerator.generateUserId());
        }
        players.add(stompPrincipal);
        return stompPrincipal;
    }


    public void cleanAllUsers() {
        if (!ListUtils.isEmpty(players)) {
            players.clear();
        }
    }


    private boolean checkMinimumPlayerNumber(int size) {
        return size >= 7;
    }

    /**
     * todo need to dynamic create roles, like more wolves and more villagers.
     *
     * @param size
     * @return
     */
    public List<Role> showRoles(int size) {
        List<Role> roleList = new ArrayList<>(size);
        roleList.add(Role.PROPHET);
        roleList.add(Role.WITCH);
        roleList.add(Role.WOLF);
        roleList.add(Role.WOLF);
        roleList.add(Role.VILLAGER);
        roleList.add(Role.VILLAGER);
        roleList.add(Role.VILLAGER);
        return roleList;
    }


}
