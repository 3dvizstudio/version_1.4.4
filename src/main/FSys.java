package main;

import processing.core.PGraphics;
import toxi.geom.Circle;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletSpring2D;
import toxi.physics2d.behaviors.AttractionBehavior2D;
import toxi.processing.ToxiclibsSupport;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@XmlRootElement(name = "flowgraph")
@XmlAccessorType(XmlAccessType.FIELD)
public class FSys {
	@XmlElement(name = "node")
	protected static ArrayList<Node> nodes = new ArrayList<>();
	@XmlElement(name = "rel")
	protected static ArrayList<Relation> relations = new ArrayList<>();
	@XmlTransient
	private HashMap<Integer, Node> nodeIndex = new HashMap<>();
	@XmlTransient
	private HashMap<Integer, ArrayList<Node>> relationIndex = new HashMap<>();
	@XmlTransient
	private Node activeNode;
	@XmlTransient
	private ArrayList<Node> selectedNodes = new ArrayList<>();

	public void build() {
		for (Node n : nodes) {n.build(); nodeIndex.put(n.id, n); }
		for (Relation r : relations) {
			ArrayList<Node> nlist = relationIndex.get(r.from);
			if (nlist == null) { nlist = new ArrayList<>(); relationIndex.put(r.from, nlist); }
			nlist.add(nodeIndex.get(r.to));
		} initPhysics();
	}
	private void initPhysics() {
		App.PSYS.getPhysics().clear();
		for (Node c : nodes) {
			App.PSYS.getPhysics().addParticle(c.verlet);
			App.PSYS.getPhysics().addBehavior(c.behavior);
		} for (Relation r : relations) {
			Node na = getNodeIndex().get(r.from);
			Node nb = getNodeIndex().get(r.to);
			float l = na.getRadius() + nb.getRadius() + 5;
			App.PSYS.getPhysics().addSpring(new VerletSpring2D(na.verlet, nb.verlet, l, 0.01f));
		}
	}
	public void update() {
//		for (Node n : nodes) n.update();
		for (Relation r : relations) {
			System.out.println(r.to + "-" + r.from);
			Node na = nodeIndex.get(r.from);
			System.out.println(na.radius);
			Node nb = nodeIndex.get(r.to);
			System.out.println(nb.radius);
			float l = (na.radius + nb.radius + 5);
			App.PSYS.getPhysics().getSpring(na.verlet, nb.verlet).setRestLength(l);
		}
	}
	public void clear() { nodes.clear(); relations.clear(); nodeIndex.clear(); relationIndex.clear(); }

	public void selectNodeNearPosition(Vec2D mousePos) {
		Circle c = new Circle(mousePos, 20);
		deselectNode();
		for (Node a : nodes) {
			if (c.containsPoint(a.verlet)) {
				setActiveNode(a);
				activeNode.verlet.lock();
				if (App.isShiftDown) { selectedNodes.add(a); } else {selectedNodes.clear(); selectedNodes.add(a);}
				break;
			}
		} if ((activeNode == null) && (!App.isShiftDown)) { selectedNodes.clear(); }
	}
	public void moveActiveNode(Vec2D mousePos) {
		if (activeNode != null) { activeNode.x = mousePos.x; activeNode.y = mousePos.y; activeNode.verlet.set(mousePos); }
	}
	public void deselectNode() {if (hasActiveNode()) {activeNode.verlet.unlock();} activeNode = null;}
	public Node getActiveNode() { return activeNode; }
	private void setActiveNode(Node a) { activeNode = a; activeNode.verlet.lock(); }
	public List<Node> getSelectedNodes() { return selectedNodes; }
	public boolean hasActiveNode() { return activeNode != null; }
	public final Node getNodeForID(int id) { return nodeIndex.get(id); }
	public final ArrayList<Node> getRelForID(int id) { return relationIndex.get(id); }
	public HashMap<Integer, Node> getNodeIndex() { return nodeIndex; }
	public void setRelations(ArrayList<Relation> relations) {FSys.relations = relations;}
	public void setNodes(ArrayList<Node> nodes) {FSys.nodes = nodes;}
	public void addNode(Node n) {nodes.add(n);}
	public void addRelation(Relation r) {relations.add(r);}
	public ArrayList<Node> getNodes() { return nodes; }

	public void draw(ToxiclibsSupport gfx) {
		if (!nodes.isEmpty()) {
			drawInfo(gfx); drawNodes(gfx); drawActive(gfx);
		}
	}
	private void drawInfo(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		pg.pushMatrix();
		pg.translate(360, 50);
		pg.fill(0xff666666);
		pg.text("id: name", 0, 0);
		pg.text("col", 50, 0);
		pg.text("size", 100, 0);
		pg.text("rad", 150, 0);
		pg.text("x", 200, 0);
		pg.text("vx", 250, 0);
		pg.text("y", 300, 0);
		pg.text("vy", 350, 0);
		pg.fill(0xff444444);
		for (Node n : nodes) {
			pg.translate(0, 10);
			pg.text(n.id + ": " + n.name, 0, 0);
			pg.text(n.color, 50, 0);
			pg.text((int) n.size, 100, 0);
			pg.text((int) n.radius, 150, 0);
			pg.text((int) n.x, 200, 0);
			pg.text((int) n.verlet.x, 250, 0);
			pg.text((int) n.y, 300, 0);
			pg.text((int) n.verlet.y, 350, 0);
		} pg.noFill();
		pg.popMatrix();
	}
	private void drawNodes(ToxiclibsSupport gfx) { for (Node n : nodes) { n.draw(gfx); } }
	private void drawActive(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		if (!selectedNodes.isEmpty()) {
			for (Node n : selectedNodes) { pg.stroke(0xffffff00); pg.ellipse(n.verlet.x, n.verlet.y, 30, 30); pg.noStroke(); }
		}
	}

	@XmlRootElement(name = "node")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Node {

		@XmlAttribute
		public String name;
		@XmlAttribute
		public int id;
		@XmlAttribute
		public float size;
		@XmlAttribute
		public float x;
		@XmlAttribute
		public float y;
		@XmlAttribute
		public int color = 180;
		@XmlAttribute
		private float radius;
		@XmlTransient
		public VerletParticle2D verlet;
		@XmlTransient
		public AttractionBehavior2D behavior;

		public Node() { }

		public void build() {
			verlet = new VerletParticle2D(x, y);
			behavior = new AttractionBehavior2D(verlet, radius, -1.2f);
			update();
			System.out.println(radius + " : " + verlet.x + " : " + verlet.y);
		}
		public void update() {
			radius = (float) ((Math.sqrt(size / Math.PI)) * App.NODE_SCALE * App.SCALE) + App.NODE_PAD;
			behavior.setRadius(radius);
			behavior.setStrength(App.NODE_STR);
		}
		public void draw(ToxiclibsSupport gfx) {
			PGraphics pg = gfx.getGraphics();
			pg.stroke(color, 100, 100);
			pg.ellipse(verlet.x, verlet.y, radius, radius);
			pg.noStroke();
			drawInfo(gfx);
		}
		private void drawInfo(ToxiclibsSupport gfx) {
			PGraphics pg = gfx.getGraphics();
			pg.fill(0xff444444);
			pg.pushMatrix();
			pg.translate(x, y);
			pg.text(id + ": " + name, -100, 0);
			pg.text(color, -100, 10);
			pg.text((int) size, -100, 20);
			pg.text((int) radius, -100, 30);
			pg.text((int) x, 100, 0);
			pg.text((int) verlet.x, 100, 10);
			pg.text((int) behavior.getAttractor().x, 100, 20);
			pg.text((int) y, 150, 0);
			pg.text((int) verlet.y, 150, 10);
			pg.text((int) behavior.getAttractor().y, 150, 20);
			pg.noFill();
			pg.popMatrix();
		}

		public float getRadius() { return radius; }
		public void setName(String name) {this.name = name;}
		public void setSize(float size) {this.size = size; update();}
		public void setColor(int color) {this.color = color;}
		public final String toString() {return Integer.toString(id);}
	}

	@XmlRootElement(name = "rel")
	public static class Relation {
		@XmlAttribute
		public int from;
		@XmlAttribute
		public int to;
		public void setTo(int to) {this.to = to;}
		public void setFrom(int from) {this.from = from;}
	}
}
