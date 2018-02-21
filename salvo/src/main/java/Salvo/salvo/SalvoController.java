package Salvo.salvo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;


//Request mapping allows for me to map certain URLs to certain methods

@RequestMapping("/api")

//Rest controller is a JSON web service which returns DATA

@RestController
public class SalvoController {

    //Autowired - import repositories im going to use

    @Autowired
    GameRepository gameRepository;

    @Autowired
    GamePlayerRepository gamePlayerRepository;

    @Autowired
    PlayerRepository playerRepository;

    @RequestMapping("/games")
    public Map<String, Object> getAllGames() {
        Map<String, Object> playerAndGames = new LinkedHashMap<>();
        Map<String, Object> currentPlayerDTO = new LinkedHashMap<>();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Player currentPlayer = getCurrentPlayer(authentication);
        currentPlayerDTO.put("id", currentPlayer.getId());
        currentPlayerDTO.put("name", currentPlayer.getUserName());

        List<Object> allGames = gameRepository.findAll().stream().map(game -> this.makeGamesDTO(game)).collect(Collectors.toList());

        playerAndGames.put("player", currentPlayerDTO);
        playerAndGames.put("games", allGames);
        return playerAndGames;
    }

    //Via DTO's - Data Transfer Objects, i can structure the way i want my data returned to me, via either
    //Maps, which are shown as Objects in JSON, or Lists, which are shown as arrays.

    //In this DTO im a using a @PathVariable, which is a SPRING annotation which allows me to
    //to use a variable as a parameter in my URL, in this case a GamePlayer ID.

    @RequestMapping("/leaderboard")
    private Map<String, Object> getAllPlayers() {
        List<Player> players = playerRepository.findAll();
        Map<String, Object> allPlayersDTO = new LinkedHashMap<>();
        List<Map> mappedPlayers = new ArrayList<>();
        players.forEach(player -> {
            Map<String, Object> myPlayer = new LinkedHashMap<>();
            List<Score> scores = player.getScores();
            double total = 0;
            int wins = 0;
            int losses = 0;
            double ties = 0;
            for (Score score : scores) {
                total += score.getScore();
                if (score.getScore() == 0) {
                    losses += 1;
                } else if (score.getScore() == 0.5) {
                    ties += 1;
                } else {
                    wins += 1;
                }
                myPlayer.put("total", total);
                myPlayer.put("wins", wins);
                myPlayer.put("losses", losses);
                myPlayer.put("ties", ties);
            }
            myPlayer.put("username", player.getUserName());
            mappedPlayers.add(myPlayer);
        });
        allPlayersDTO.put("players", mappedPlayers);
        return allPlayersDTO;
    }

    @RequestMapping("/game_view/{gamePlayerId}")
    private ResponseEntity<Object> findGamePlayer(@PathVariable Long gamePlayerId) {

        //By putting in a ID in the URL, i can access the specific INSTANCE of gameplayer
        //which i want to see, by using findOne in the game player repository.

        GamePlayer gameplayer = gamePlayerRepository.findOne(gamePlayerId);

        //Accessing this INSTANCE of GamePlayer related Game INSTANCE.

        Game game = gameplayer.getGame();

        //Creating a LinkedHashMap for structuring my DTO.

        Map<String, Object> gameViewDTO = new LinkedHashMap<>();

        //Inserting keys and values

        gameViewDTO.put("id", game.getId());
        gameViewDTO.put("created", new Date());

        //Creating a new List to be represented as an Array in my JSON.

        List<Map> gamePlayerList = new ArrayList<>();

        //Retrieving a Set of GamePlayers by using my Game instance's getGamePlayers method,
        //which return a Set of the two GamePlayers that are related.

        Set<GamePlayer> gameplayers = game.getGameplayers();

        //Looping  through my two GamePlayers, and for each one creating a Map with the needed information.
        //In this case, another method is being referenced as a Value (makePlayerDTO).

        gameplayers.forEach(gamePlayer -> {
            HashMap<String, Object> gamePlayerMap = new HashMap<>();
            gamePlayerMap.put("id", gamePlayer.getId());
            gamePlayerMap.put("player", makePlayersDTO(gamePlayer.getPlayer()));
            gamePlayerList.add(gamePlayerMap);
        });

        //I continue to put the remaining needed info, this time referencing a different method for creating
        //my SalvoDTO.

        gameViewDTO.put("gamePlayers", gamePlayerList);
        gameViewDTO.put("ships", gameplayer.getShips());
        gameViewDTO.put("salvoes", makeSalvoDTO(game));

        boolean isLoggedIn = false;
        HttpStatus status = HttpStatus.OK;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Player currentPlayer = getCurrentPlayer(authentication);
        List<GamePlayer> currentGamePlayers = currentPlayer.getGameplayers();
        List<Long> gamePlayersIds = new ArrayList<>();

        currentGamePlayers.forEach(gamePlayer -> {
            gamePlayersIds.add(gamePlayer.getId());
        });

        for (Long gamePlayersId : gamePlayersIds) {
            if (gamePlayersId == gamePlayerId) {
                isLoggedIn = true;
                break;
            }
        }

        if (!isLoggedIn) {
            gameViewDTO.clear();
            gameViewDTO.put("status", "Not your game! Stop cheating!");
            status = HttpStatus.FORBIDDEN;
        }

        return new ResponseEntity<>(gameViewDTO, status);
    }

    private Map<String, Object> makePlayersDTO(Player player) {
        Map<String, Object> playersDTO = new LinkedHashMap<String, Object>();
        playersDTO.put("id", player.getId());
        playersDTO.put("username", player.getUserName());
        return playersDTO;
    }

    private Map<String, Object> makeGamesDTO(Game game) {

        //Instantiate a new LinkedHashMAp with the name gameDTO

        Map<String, Object> gameDTO = new LinkedHashMap<String, Object>();

        //LinkedHashMap allows me to structure how i want my response DATA
        //I now "PUT" two keys with two values

        gameDTO.put("id", game.getId());
        gameDTO.put("created", game.getDate());

        //Creating a new List to be represented as an Array in my JSON.

        List<Map> gamePlayerList = new ArrayList<>();

        //Save gameplayers of SPECIFIC game to local variable

        Set<GamePlayer> gameplayers = game.getGameplayers();


        //Loop through my gamePlayers , and for each one restructure it with HashMap into object notation
        //With the values i choose. In this case, one of my values - makePlayersDTO, is a method
        //creating another DTO. This is done this way purely for structural reasons.

        gameplayers.forEach(gamePlayer -> {
            HashMap<String, Object> gamePlayerMap = new HashMap<>();
            gamePlayerMap.put("id", gamePlayer.getId());
            gamePlayerMap.put("player", makePlayersDTO(gamePlayer.getPlayer()));
            gamePlayerMap.put("score", gamePlayer.getScore(gamePlayer.getGame()));
            //Add my structured data into the previously created List.

            gamePlayerList.add(gamePlayerMap);
        });


        //Add that data into my Original LinkedHashMap - gameDTO.
        gameDTO.put("gamePlayers", gamePlayerList);
        gameDTO.put("gameOver", gameOver(game));
        return gameDTO;
    }

    private Map<String, Object> makeSalvoDTO(Game game) {

        //In this method for creating a DTO, im using a nested loop.
        //First, im accesing my two GamePlayers via the specifc Game INSTANCE.
        //I then loop through each one, and while inside, i loop through its Salvoes, and construct them
        //into a Map. I later use this map as a value in my Game_View DTO.

        Map<String, Object> playerMap = new LinkedHashMap<>();
        Set<GamePlayer> gameplayers = game.getGameplayers();

        gameplayers.forEach(gameplayer -> {
            String playerID = Long.toString(gameplayer.getPlayer().getId());
            Set<Salvo> salvoes = gameplayer.getSalvoes();
            Map<String, Object> salvoMap = new LinkedHashMap<>();

            salvoes.forEach(salvo -> {
                String salvoTurn = Long.toString(salvo.getTurn());
                salvoMap.put(salvoTurn, salvo.getSalvoLocations());
                playerMap.put(playerID, salvoMap);
            });
        });
        return playerMap;
    }

    private Player getCurrentPlayer(Authentication authentication) {
        List<Player> players = playerRepository.findByUserName(authentication.getName());
        Player myPlayer = new Player();
        if (!players.isEmpty()) {
            Player player = players.get(0);
            myPlayer = player;
        } else {
            isGuest(authentication);
        }
        return myPlayer;
    }

    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    @RequestMapping(path = "/players", method = RequestMethod.POST)
    private ResponseEntity<Map<String, Object>> signUpPlayer(@RequestParam String username, @RequestParam String password) {
        if (username.isEmpty()) {
            return new ResponseEntity<>(makeMap("error", "No name"), HttpStatus.FORBIDDEN);
        }
        Player player = playerRepository.findOneByUserName(username);
        if (player != null) {
            return new ResponseEntity<>(makeMap("error", "Name is already used"), HttpStatus.CONFLICT);
        }
        player = playerRepository.save(new Player(username, password));
        return new ResponseEntity<>(makeMap("name", player.getUserName()), HttpStatus.CREATED);
    }

    private Map<String, Object> makeMap(String key, Object value) {
            Map<String, Object> map = new HashMap<>();
            map.put(key, value);
            return map;
    }

    @RequestMapping(path = "/games", method = RequestMethod.POST)
    private ResponseEntity<Object> createGame (Authentication authentication) {
        Player currentPlayer = getCurrentPlayer(authentication);
        Game newGame = new Game(new Date());
        gameRepository.save(newGame);
        GamePlayer newGamePlayer = new GamePlayer(currentPlayer, newGame, new Date());
        gamePlayerRepository.save(newGamePlayer);
        Map<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("succes", "New game created!");
        responseMap.put("GPID", newGamePlayer.getId());
        return new ResponseEntity<>(responseMap, HttpStatus.CREATED);
    }

    @RequestMapping(path = "/game/{gameId}/players", method = RequestMethod.POST)
    private ResponseEntity<Object> joinGame (@PathVariable Long gameId, Authentication authentication) {
        Player currentPlayer = getCurrentPlayer(authentication);

        if (currentPlayer.getUserName() == null) {
            return new ResponseEntity<>("No User", HttpStatus.UNAUTHORIZED);
        }

        if (gameRepository.findOne(gameId) == null) {
            return new ResponseEntity<>("No Game", HttpStatus.FORBIDDEN);
        }
        Game game = gameRepository.findOne(gameId);

        List<Player> players = game.getPlayers();
        if (players.size() == 2) {
            return new ResponseEntity<>("Game is full", HttpStatus.FORBIDDEN);
        }


        GamePlayer gameplayer = new GamePlayer(currentPlayer, game, new Date());
        gamePlayerRepository.save(gameplayer);
        Map<String,Object> gpIdMap = new LinkedHashMap<>();
        gpIdMap.put("gpid", gameplayer.getId());
        return new ResponseEntity<>(gpIdMap, HttpStatus.CREATED);
    }

    private boolean gameOver (Game game) {
        List<Score> scores = game.getScores();

        boolean gameOver = false;
        if (scores.size() != 0){
            gameOver = true;
        }
        return gameOver;
    }
}
