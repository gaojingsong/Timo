package fm.liu.timo.manager.response;

import java.util.ArrayList;
import java.util.Collection;
import fm.liu.timo.TimoServer;
import fm.liu.timo.backend.Node;
import fm.liu.timo.backend.Source;
import fm.liu.timo.config.model.Datasource;
import fm.liu.timo.manager.handler.ShowHandler;
import fm.liu.timo.manager.response.ResponseUtil.Head;

/**
 * @author liuhuanting
 */
public class ShowDatanode extends ShowHandler {
    private static final ArrayList<Head> heads = new ArrayList<Head>();

    static {
        heads.add(new Head("node"));
        heads.add(new Head("source"));
        heads.add(new Head("source_id"));
        heads.add(new Head("type"));
        heads.add(new Head("idle_size", "active connection size of current source"));
        heads.add(new Head("total_size", "total connection size of current source"));
        heads.add(new Head("strategy"));
    }

    @Override
    public String getInfo() {
        return "show status of datanodes in timo-server";
    }

    @Override
    public ArrayList<Head> getHeads() {
        return heads;
    }

    @Override
    public ArrayList<Object[]> getRows() {
        ArrayList<Object[]> rows = new ArrayList<>();
        Collection<Node> nodes = TimoServer.getInstance().getConfig().getNodes().values();
        for (Node node : nodes) {
            Object[] row = new Object[heads.size()];
            int i = 0;
            row[i++] = node.getID();
            Source source = node.getSource();
            Datasource config = source.getConfig();
            row[i++] = config.getHost() + ":" + config.getPort() + "/" + config.getDB();
            row[i++] = config.getID();
            row[i++] = config.getType();
            row[i++] = source.getIdleSize();
            row[i++] = source.getSize();
            row[i++] = node.getStrategy();
            rows.add(row);
        }
        return rows;
    }

}
