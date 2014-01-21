package util;

import main.FSys;

import java.util.ArrayList;

public class RGen {
	private ArrayList<FSys.Node> nodes = new ArrayList<>();
	private ArrayList<FSys.Relation> relations = new ArrayList<>();
	private int count;
	private float size;
	private int index;
	private FSys.Node parentNode;

	public RGen(FSys.Node parentNode, int index, int count, float size, boolean isDivide) {
		this.parentNode = parentNode;
		this.index = index;
		this.count = count;
		if (isDivide) {
			this.size = parentNode.size / count;
			parentNode.size = this.size;
		} else { this.size = size; }
		build();
	}
	private void build() {
		for (int i = 0; i < count; i++) {
			FSys.Node n = new FSys.Node();
			n.id = index++;
			n.name = parentNode.name + "." + n.id;
			n.size = size;
			n.color = parentNode.color;
			n.x = parentNode.x;
			n.y = parentNode.y;
			n.build();
			nodes.add(n);

			FSys.Relation r = new FSys.Relation();
			r.setFrom(n.id);
			r.setTo(parentNode.id);
			relations.add(r);
		}
	}

	public ArrayList<FSys.Node> getNodes() {return nodes;}
	public ArrayList<FSys.Relation> getRelations() { return relations; }
}
