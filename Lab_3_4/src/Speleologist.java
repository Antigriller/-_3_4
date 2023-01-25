import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Objects;

public class Speleologist extends Agent {

    public static String GO_INSIDE = "go_inside";
    public static String WUMPUS_WORLD_TYPE = "wumpus-world";
    public static String NAVIGATOR_AGENT_TYPE = "navigator-agent";

    public static String WORLD_DIGGER_CONVERSATION_ID = "digger-world";
    public static String NAVIGATOR_DIGGER_CONVERSATION_ID = "digger-navigator";

    private int arrowCount = 1;
    private AID wumpusWorld;
    private AID navigationAgent;
    private String currentWorldState = "";

    @Override
    protected void setup() {
        System.out.println("Hello! Speleologist-agent "+getAID().getName()+" is ready.");
        addBehaviour(new WumpusWorldFinder());
    }

    private class WumpusWorldFinder extends Behaviour {
        private int step = 0;
        @Override
        public void action() {
            if (step == 0){
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(WUMPUS_WORLD_TYPE);
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result != null && result.length > 0) {
                        wumpusWorld = result[0].getName();
                        myAgent.addBehaviour(new WumpusWorldPerformer());
                        ++step;
                    } else {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }
        }
        @Override
        public boolean done() {
            return step == 1;
        }
    }
    private class WumpusWorldPerformer extends Behaviour {
        private MessageTemplate mt;

        private int step = 0;
        @Override
        public void action() {
            switch (step) {
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(wumpusWorld);
                    cfp.setContent(GO_INSIDE);
                    cfp.setConversationId(WORLD_DIGGER_CONVERSATION_ID);
                    cfp.setReplyWith("cfp"+System.currentTimeMillis());
                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(WORLD_DIGGER_CONVERSATION_ID),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            String answer = reply.getContent();
                            currentWorldState = answer;
                            myAgent.addBehaviour(new NavigatorAgentPerformer());
                            step = 2;
                        }
                    }
                    else {
                        block();
                    }
                    break;
            }
        }
        @Override
        public boolean done() {
            return step == 2;
        }
    }
    private class NavigatorAgentPerformer extends Behaviour {
        private int step = 0;
        private MessageTemplate mt;
        @Override
        public void action() {
            switch (step) {
                case 0: {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(NAVIGATOR_AGENT_TYPE);
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        if (result != null && result.length > 0) {
                            navigationAgent = result[0].getName();
                            ++step;
                        } else {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                }
                case 1: {
                    ACLMessage order = new ACLMessage(ACLMessage.INFORM);
                    order.addReceiver(navigationAgent);
                    order.setContent(currentWorldState);
                    order.setConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID);
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 2;
                }
                case 2: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            String actions = reply.getContent();
                            actions = actions.substring(1, actions.length()-1);
                            String[] instructions = actions.split(", ");
                            if (instructions.length == 1){
                                sendMessage("take","");
                            }
                            else if (instructions.length == 2 && Objects.equals(instructions[1], "shoot")){
                                sendMessage("shoot",instructions[0]);
                            }
                            else if (instructions.length == 2 && Objects.equals(instructions[1], "move")){
                                sendMessage("move",instructions[0]);
                            }
                            else {
                                System.out.println("ERROR ACTIONS");
                            }
                            ++step;
                        }
                    }
                    else {
                        block();
                    }
                    break;

                }
                case 3:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        currentWorldState = reply.getContent();
                        step = 1;
                    }
                    else {
                        block();
                    }
                    break;
            }
        }
        @Override
        public boolean done() {
            return step == 4;
        }

        private void sendMessage (String type, String content){
            //shoot, take, move
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(wumpusWorld);
            msg.setLanguage("English");
            msg.setContent(content);
            msg.setConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID);
            msg.setReplyWith("order"+System.currentTimeMillis()); // Unique value
            myAgent.send(msg);
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID),
                    MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
        }
}}