
// Create class for handling all methods in a Object Oriented approach
function DataHandler() {

    //Initialize variables
    this.playerId;
    this.playerData;
    this.gamePlayerID;
    this.shipsLocations;
    this.mySalvoes;
    this.myGrid = {
        0: "",
        1: "A",
        2: "B",
        3: "C",
        4: "D",
        5: "E",
        6: "F",
        7: "G",
        8: "H",
        9: "I",
        10: "J"
    }

    //Init function which initializes the first methods which will chain
    //on to the rest of the methods at the appropriate times.

    this.init = function() {
        this.createGrid("#container1", "grid1");
        this.createGrid("#container2", "grid2");
        this.getGameData();
        this.logOut();
    };

    //Method for retrieving data, and then manipulating accordingly in the CallBack function.

    this.getGameData = function() {
        var that = this;

        //Acquire my GamePlayer ID via the getParameterByName method, which uses Regular Expressions to sort out URL

        this.gamePlayerID = this.getParameterByName('gp');
        $.getJSON('http://localhost:8080/api/game_view/' + this.gamePlayerID, function(data) {
        // Saving data to variables
            that.mySalvoes = data.salvoes;
            that.shipsLocations = data.ships;
            that.playerData = data.gamePlayers;

        // Running other methods once data has been retrieved.
            that.getPlayerId();
            that.displayPlayerData();
            that.appendShips();

        });
    };

    //Loop through GamePlayers, and compare GP ID to the URL param, if true, enter object and
    //assign player.id to variable, which will be later used to assign Salvoes to correct Player.

    this.getPlayerId = function(){
        var that = this;
        $.each(this.playerData, function(gpIndex, gp){
            if(gp.id == that.gamePlayerID) {
                that.playerId = gp.player.id;
            };
        });
    };

    //Loop through ships array and color grid cells accordingly

    this.appendShips = function(){
        $.each(this.shipsLocations, function(i, e) {
            if (e.type == "PatrolBoat") {
                $.each(e.locations, function(i, e) {
                    $('#container1 .' + e).css('background', 'blue').addClass('ship');
                });
            } else if (e.type == "Battleship") {
                $.each(e.locations, function(i, e) {
                    $('#container1 .' + e).css('background', 'green').addClass('ship');
                });
            } else if (e.type == "Cruiser") {
                $.each(e.locations, function(i, e) {
                    $('#container1 .' + e).css('background', 'orange').addClass('ship');
                });
            } else if (e.type == "Destroyer") {
                $.each(e.locations, function(i, e) {
                    $('#container1 .' + e).css('background', 'darkgreen').addClass('ship');
                });
            } else if (e.type == "Submarine") {
                $.each(e.locations, function(i, e) {
                    $('#container1 .' + e).css('background', 'pink').addClass('ship');
                });
            };
        });
    this.colorSalvoes();
    };

    //Loop through Salvoes  and color grid cells accordingly with Turn number.
    //Inception shit going on here :O

    this.colorSalvoes = function(){
        var that = this;
        $.each(this.mySalvoes, function(index,object){
            if (that.playerId == index) {
                $.each(object, function(i,element){
                    $.each(element, function(number,salvo){
                        $("#container2 ." + salvo).css("background", "red").text(i);
                    });
                });
            } else {
                $.each(object, function(i,element){
                    $.each(element, function(number,salvo){
                        if  ($("#container1 ." + salvo).hasClass("ship")) {
                            $("#container1 ." + salvo).css("background", "red").text(i);
                        };
                    });
                });
            };
        });
    };

    //Method for getting GP ID

    this.getParameterByName = function(name, url) {
        if (!url) url = window.location.href;
        name = name.replace(/[\[\]]/g, "\\$&");
        var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, " "));
    }

    //Create nested for loops for creating columns and rows with divs, while attaching
    // classes needed for later identification of single Grid cells.

    this.createGrid = function(container, grid) {
        for (var rows = 0; rows < 11; rows++) {
            var div = $('<div>').addClass('column ' + rows)
            for (var columns = 0; columns < 11; columns++) {
                div.append($("<div>").addClass(grid));
                $(container).append(div);
                if (rows == 0) {
                    $("." + grid).addClass('header');
                };
            };
        };
        $("." + grid).width(500 / 11);
        $("." + grid).height(500 / 11);
        this.addLetters(container);
        this.addNumbers(container);

    };

    //Via the container parameter given in createGrid(), I insert numbers into the correct
    // grid cells for organizing the Grid, by taking each First() of each Child() of a column,
    //therefor the top row of every grid.

    this.addNumbers = function(container) {
        $(container + ' .column').each(function(i) {
            $(this).children().first().text(i);
        });
        $(container + ' .column').children().first().text("");
        this.giveParametersToMapClasses(container);
    }

    //Same idea as previous method, except using myGrid which is a object containing the needed letters
    //for the grid.

    this.addLetters = function(container) {
        var that = this;
        $(container + ' .header').each(function(i) {
            $(this).append($('<span>').text(that.myGrid[i]));
        });

    };

    //Simple for loop which uses the index number of the iteration and the container parameter
    //from the initial createGrid() method to run the mapID() method with correct arguments
    //for assigning classes to single grid cells, which will allows matching salvoes and ships later on.

    this.giveParametersToMapClasses = function(container) {
        var that = this;
        for (var i = 1; i <= 10; i++) {
            that.mapGridCellsClasses(container, i);
        };
    };

    //By taking the index number and container parameter from above method, I loop through all grid cells
    //and assign classes based on position.

    this.mapGridCellsClasses = function(container, columnClass) {
        var that = this;
        $(container + " ." + columnClass).children().each(function(i, e) {
            $(this).addClass(that.myGrid[i] + columnClass);
        });
    };

    //I loop through my two players, recognize via the URL param which is the one
    //playing, and append usernames to HTML.

    this.displayPlayerData = function() {
    var that = this;
        $.each(this.playerData, function(i, e) {
                        if (e.id == that.gamePlayerID) {
                            $('#player1').text(e.player.username + "(YOU)");
                        } else {
                            $('#player2').text(e.player.username);
                        };
        });
    };

    this.logOut = function(){
    $("#logOutGame").on("click", function(){
                $.post("/api/logout")
                .done(function() {
                alert("You have been logged out.");
                window.location.href = "/web/public/games.html";
                    $("#playerName").hide();
                    $("#logOut").hide();
                    $("#username").val("");
                    $("#password").val("");
                    $("#loginForm").show();
                });
            });
    }

};

var dataHandler = new DataHandler();
dataHandler.init();