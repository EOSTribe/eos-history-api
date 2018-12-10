//import org.zeromq.jeromq;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.Arrays;

public class Main {

    int messageType;


    public void decodeMessage(byte[] message){
        //get

    }


    public static void main(String[] args) {
        ZMQ.Context context = ZMQ.context(1);
        //  Socket to talk to server

        ZMQ.Socket requester = context.socket(ZMQ.PULL);
        requester.connect("tcp://10.0.64.13:5556");
        requester.setReceiveBufferSize(10240);

//        byte[] rpl = requester.recv();

            while (true) {
                try {
                    byte[] reply = requester.recv();

                    byte typeOfmessage = reply[0];

                    byte[] json=  Arrays.copyOfRange(reply, 8, reply.length  );

                    System.out.println("Received " + new String(json, "UTF-8") + " ");
                } catch (Exception e) {
                    System.out.println(e);
                }

            }
    }
}
