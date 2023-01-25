import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Environment extends Agent {

    public static String SERVICE_DESCRIPTION = "WUMPUS-WORLD";
    private static int START = -1;
    //    private static int EMPTY = 0;
    private static int WUMPUS = 1;
    private static int PIT = 2;
    private static int BREEZE = 3;
    private static int STENCH = 4;
    private static int SCREAM = 5;
    private static int GOLD = 6;
    private static int BUMP = 7;
    public static HashMap<Integer, String> roomCodes = new HashMap<Integer, String>() {{
        put(START, Navigator.START);
        put(WUMPUS, Navigator.WUMPUS);
        put(PIT, Navigator.PIT);
        put(BREEZE, Navigator.BREEZE);
        put(STENCH, Navigator.STENCH);
        put(SCREAM, Navigator.SCREAM);
        put(GOLD, Navigator.GOLD);
        put(BUMP, Navigator.BUMP);
    }};
    private static int NUM_OF_ROWS = 4;
    private static int NUM_OF_COLUMNS = 4;

    private Room[][] wumpusMap;
    private HashMap<AID, Coords> Speleologists;

    String nickname = "WumpusWorld";
    AID id = new AID(nickname, AID.ISLOCALNAME);

    @Override
    protected void setup() {
        System.out.println("Hello! Environment-agent "+getAID().getName()+" is ready.");
        Speleologists = new HashMap<>();
        generateMap();
        showMap();
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Speleologist.WUMPUS_WORLD_TYPE);
        sd.setName(SERVICE_DESCRIPTION);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new SpeleologistConnectPerformer());
        addBehaviour(new SpeleologistArrowPerformer());
        addBehaviour(new SpeleologistGoldPerformer());
        addBehaviour(new SpeleologistMovePerformer());
    }

    private void generateMap() {
        this.wumpusMap = new Room[NUM_OF_ROWS][NUM_OF_COLUMNS];
        this.wumpusMap[0][0] = new Room();
        this.wumpusMap[0][1] = new Room(BREEZE);
        this.wumpusMap[0][2] = new Room(PIT);
        this.wumpusMap[0][3] = new Room(BREEZE);
        this.wumpusMap[1][0] = new Room(STENCH);
        this.wumpusMap[1][3] = new Room(BREEZE);
        this.wumpusMap[2][0] = new Room(WUMPUS, STENCH);
        this.wumpusMap[2][1] = new Room(BREEZE, STENCH, GOLD);
        this.wumpusMap[2][2] = new Room(PIT);
        this.wumpusMap[2][3] = new Room(BREEZE);
        this.wumpusMap[3][0] = new Room(STENCH);
        this.wumpusMap[3][2] = new Room(BREEZE);
        this.wumpusMap[3][3] = new Room(PIT);
        for (int i=0; i < this.wumpusMap.length; i++){
            for (int j= 0; j < this.wumpusMap[i].length; j++){
                if (this.wumpusMap[i][j] == null) {
                    this.wumpusMap[i][j] = new Room();
                }
            }

        }

    }
    private void showMap(){
        for (int i=0; i < this.wumpusMap.length; i++){
            for (int j= 0; j < this.wumpusMap[i].length; j++){
                System.out.println("POSITION: " + i + ", " + j + "; MARKERS: " + wumpusMap[i][j].events);
            }

        }
    }
    private class SpeleologistConnectPerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String message = msg.getContent();
                if (Objects.equals(message, Speleologist.GO_INSIDE)){
                    AID current_Speleologist = msg.getSender();
                    Coords Speleologist_coords = Speleologists.get(current_Speleologist);
                    if (Speleologist_coords == null){
                        Speleologists.put(current_Speleologist, new Coords(0, 0));
                    }
                    else {
                        Speleologists.put(current_Speleologist, new Coords(0, 0));
                    }
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent(wumpusMap[0][0].events.toString());
                    myAgent.send(reply);
                }
//
            }
            else {
                block();
            }
        }
    }
    private class SpeleologistArrowPerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(Speleologist.SHOOT_ARROW);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(Speleologist.SHOOT_ARROW);

                String message = msg.getContent();
                AID current_Speleologist = msg.getSender();
                Coords Speleologist_coords = Speleologists.get(current_Speleologist);

                int row = Speleologist_coords.row;
                int column = Speleologist_coords.column;
                String answer = "";
                if (message.equals("down")){
                    for (int i = 0; i < row; ++i){
                        if (wumpusMap[i][column].events.contains(Environment.roomCodes.get(WUMPUS))){
                            answer = Navigator.SCREAM;
                        }
                    }
                }
                else if(message.equals("up")){
                    for (int i = row+1; i < NUM_OF_ROWS; ++i){
                        if (wumpusMap[i][column].events.contains(Environment.roomCodes.get(WUMPUS))){
                            answer = Navigator.SCREAM;
                        }
                    }
                }
                else if(message.equals("left")){
                    for (int i = 0; i < column; ++i){
                        if (wumpusMap[row][i].events.contains(Environment.roomCodes.get(WUMPUS))){
                            answer = Navigator.SCREAM;
                        }
                    }
                }
                else if (message.equals("right")){
                    for (int i = column+1; i < NUM_OF_COLUMNS; ++i){
                        if (wumpusMap[row][i].events.contains(Environment.roomCodes.get(WUMPUS))){
                            answer = Navigator.SCREAM;
                        }
                    }
                }

                reply.setContent(answer);

                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }
    private class SpeleologistMovePerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(Speleologist.MOVE);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(Speleologist.MOVE);

                String message = msg.getContent();
                AID current_Speleologist = msg.getSender();
                Coords Speleologist_coords = Speleologists.get(current_Speleologist);
                System.out.println("World say: Current agent coords: " + Speleologist_coords.row + " | " + Speleologist_coords.column);
                if (Speleologist_coords == null){
                    Speleologists.put(current_Speleologist, new Coords(0, 0));
                    Speleologist_coords = Speleologists.get(current_Speleologist);
                }
                int row = Speleologist_coords.row;
                int column = Speleologist_coords.column;
                if (message.equals("down")){
                    row -= 1;
                }
                else if(message.equals("up")){
                    row += 1;
                }
                else if(message.equals("left")){
                    column -=1;
                }
                else if (message.equals("right")){
                    column += 1;
                }
                if (row > -1 && column > -1 && row < NUM_OF_ROWS && column < NUM_OF_COLUMNS){
                    Speleologist_coords.column = column;
                    Speleologist_coords.row = row;
                    reply.setContent(wumpusMap[row][column].events.toString());
                }
                else {
                    reply.setContent(String.valueOf(new ArrayList<String>(){{
                        add(Navigator.BUMP);
                    }}));
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }
    private class SpeleologistGoldPerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(Speleologist.TAKE_GOLD);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String message = msg.getContent();
                AID current_Speleologist = msg.getSender();
                Coords Speleologist_coords = Speleologists.get(current_Speleologist);
                if (Speleologist_coords == null){
                    Speleologists.put(current_Speleologist, new Coords(0, 0));
                }
                else {
                    if (wumpusMap[Speleologist_coords.row][Speleologist_coords.column].events.contains(Environment.roomCodes.get(GOLD))){
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(Speleologist.TAKE_GOLD);
                        reply.setContent("GOLD");
                        myAgent.send(reply);
                    }
                }
            }
            else {
                block();
            }
        }
    }
}
class Room {
    ArrayList<String> events = new ArrayList<>();
    Room (int... args){
        for (int i: args){
            events.add(Environment.roomCodes.get(i));
        }
    }
}
class Coords {
    int row = 0;
    int column = 0;
    Coords(int row, int column){
        this.row = row;
        this.column = column;
    }
}
