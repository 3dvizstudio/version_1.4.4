package main;

import processing.core.*;
import toxi.geom.Circle;
import toxi.geom.Rect;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletSpring2D;
import toxi.physics2d.behaviors.AttractionBehavior2D;
import toxi.processing.ToxiclibsSupport;
import util.Color;

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
			VerletSpring2D s = new VerletSpring2D(na.verlet, nb.verlet, l, 0.01f);
			App.PSYS.getPhysics().addSpring(s);
//			App.PSYS.addSpring(na, nb);
		}
	}
	public void update() {
		for (Relation r : relations) {
			System.out.println(r.to + "-" + r.from);
			Node na = nodeIndex.get(r.from);
			System.out.println(na.radius);
			Node nb = nodeIndex.get(r.to);
			System.out.println(nb.radius);
			float l = (((na.radius + nb.radius + 5) * App.SPR_SCALE));
			App.PSYS.getPhysics().getSpring(na.verlet, nb.verlet).setRestLength(l).setStrength(App.SPR_STR);
		}
		for (FSys.Node n : nodes) {
			n.behavior.setRadius(n.getRadius() * 2);
			n.behavior.setStrength(App.NODE_STR);
		}
	}
	public void updateNodes() {
		for (FSys.Node n : nodes) { n.update(); }
	}
	public void updateSprings() {
		for (Relation r : relations) {
			Node na = nodeIndex.get(r.from);
			Node nb = nodeIndex.get(r.to);
			float l = (((na.radius + nb.radius + 5) * App.SPR_SCALE));
			App.PSYS.getPhysics().getSpring(na.verlet, nb.verlet).setRestLength(l).setStrength(App.SPR_STR);
		}
	}

	public void draw(ToxiclibsSupport gfx) {
		if (!nodes.isEmpty()) {
			drawRelations(gfx); drawNodes(gfx); drawActive(gfx);
			if (App.SHOW_INFO) drawInfo(gfx);
		}
	}
	private void drawRelations(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		pg.stroke(0xff2b2b2b);
		pg.strokeWeight(2);
		for (Relation r : relations) {
			gfx.line(getNodeIndex().get(r.from).verlet, getNodeIndex().get(r.to).verlet);
		}
		pg.strokeWeight(1);
		pg.noStroke();
	}
	private void drawInfo(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		float totalSize = 0;
		for (Node n : nodes) { totalSize += n.size; }
		pg.pushMatrix();
		pg.translate(App.P5.width - 120, 50);
		pg.fill(Color.BG_TEXT);
		pg.text("NAME", 10, -2);
		pg.textAlign(PApplet.RIGHT);
		pg.text("AREA", 100, -2);
		int stripe = 0;
		for (Node n : nodes) {
			if (stripe % 2 == 0) { pg.fill(0xff383838); } else {pg.fill(0xff333333);}
			gfx.rect(Rect.fromCenterExtent(new Vec2D(53, 6), new Vec2D(50, 5)));
			pg.fill(Color.FACES);
			gfx.rect(Rect.fromCenterExtent(new Vec2D(0, 6), new Vec2D(3, 5)));
			pg.fill(n.color, 100, 100);
			gfx.rect(Rect.fromCenterExtent(new Vec2D(0, 5), new Vec2D(1, 4)));
			if (n == activeNode) pg.fill(Color.ACTIVE);
			else if (selectedNodes.contains(n)) pg.fill(Color.SELECTED);
			else pg.fill(Color.BG_TEXT);
			pg.translate(0, 10);
			pg.textAlign(PApplet.LEFT);
			pg.text(n.name, 10, 0);
			pg.textAlign(PApplet.RIGHT);
			pg.text(n.id, -10, 0);
			pg.text((int) n.size, 100, 0);
			stripe++;
		}
		pg.fill(Color.ACTIVE);
		pg.textAlign(PApplet.RIGHT);
		pg.text("Total Area", 100, 20);
		pg.text(App.DF3.format(totalSize) + " sq.m", 100, 30);
		pg.noFill();
		pg.popMatrix();
	}
	private void drawNodes(ToxiclibsSupport gfx) { for (Node n : nodes) { n.draw(gfx); } }
	private void drawActive(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		if (hasActiveNode()) {
			activeNode.drawDebugInfo(gfx);
			activeNode.drawHighlightAct(gfx);
		}
		if (!selectedNodes.isEmpty()) {
			for (Node n : selectedNodes) { n.drawHighlightSel(gfx); }
		}
	}
	private void drawDebugInfo(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		pg.pushMatrix();
		pg.translate(355, 50);
		pg.fill(Color.BG_TEXT);
		pg.text("ID", 10, 0);
		pg.text("col", 150, 0);
		pg.text("rad", 250, 0);
		pg.text("x", 300, 0);
		pg.text("vx", 350, 0);
		pg.text("y", 400, 0);
		pg.text("vy", 450, 0);
		for (Node n : nodes) {
			pg.fill(n.color, 100, 100);
			gfx.rect(Rect.fromCenterExtent(new Vec2D(-7, 5), new Vec2D(2, 4)));
			if (n == activeNode) pg.fill(Color.ACTIVE);
			else if (selectedNodes.contains(n)) pg.fill(Color.SELECTED);
			else pg.fill(Color.BG_TEXT);
			pg.translate(0, 10);
			pg.text(n.id, 25, 0);
			pg.text((int) n.size, 200, 0);
			pg.text((int) n.radius, 250, 0);
			pg.text((int) n.x, 300, 0);
			pg.text((int) n.verlet.x, 350, 0);
			pg.text((int) n.y, 400, 0);
			pg.text((int) n.verlet.y, 450, 0);
		} pg.noFill();
		pg.popMatrix();
	}
	public void addNode(Node n) {nodes.add(n);}
	public void addRelation(Relation r) {relations.add(r);}
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
	public void setRelations(ArrayList<Relation> relations) {FSys.relations = relations;}
	public void setNodes(ArrayList<Node> nodes) {FSys.nodes = nodes;}
	public void clear() { nodes.clear(); relations.clear(); nodeIndex.clear(); relationIndex.clear(); }
	public void setActiveNode(Node a) { activeNode = a; a.update(); activeNode.verlet.lock(); }
	public final Node getNodeForID(int id) { return nodeIndex.get(id); }
	public final ArrayList<Node> getRelForID(int id) { return relationIndex.get(id); }
	public boolean hasActiveNode() { return activeNode != null; }
	public Node getActiveNode() { return activeNode; }
	public List<Node> getSelectedNodes() { return selectedNodes; }
	public ArrayList<Node> getNodes() { return nodes; }
	public HashMap<Integer, Node> getNodeIndex() { return nodeIndex; }
	public ArrayList<Relation> getRelations() { return relations; }

	@XmlRootElement(name = "node")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Node {

		@XmlAttribute
		public String name;
		@XmlAttribute
		public int id;
		@XmlAttribute
		public float size = 50;
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
			this.x = verlet.x;
			this.y = verlet.y;
			radius = (float) ((Math.sqrt(size / Math.PI)) * App.NODE_SCALE * App.SCALE) + App.NODE_PAD;
			behavior.setRadius(radius * 2);
			behavior.setStrength(App.NODE_STR);
		}
		public void synchronize() {
			this.x = verlet.x;
			this.y = verlet.y;
		}

		public void draw(ToxiclibsSupport gfx) {
			PGraphics pg = gfx.getGraphics();
			synchronize();
			pg.stroke(Color.BLACK);
			pg.fill(Color.CP5_BG);
			pg.ellipse(verlet.x, verlet.y, 3, 3);
			pg.noFill();
			pg.stroke(Color.BG_TEXT);
			pg.ellipse(verlet.x, verlet.y, radius - 2, radius - 2);
			pg.noStroke();
			drawInfo(gfx);
		}
		private void drawColored(ToxiclibsSupport gfx) {
			PGraphics pg = gfx.getGraphics();
			pg.stroke(color, 100, 100);
			pg.ellipse(verlet.x, verlet.y, radius, radius);
			pg.noStroke();
		}
		private void drawHighlightSel(ToxiclibsSupport gfx) {
			PGraphics pg = gfx.getGraphics();
			pg.stroke(Color.BLACK);
			pg.fill(Color.SELECTED);
			pg.ellipse(x, y, 3, 3);
			pg.noStroke();
			pg.noFill();
		}
		private void drawHighlightAct(ToxiclibsSupport gfx) {
			PGraphics pg = gfx.getGraphics();
			pg.stroke(Color.BLACK);
			pg.ellipse(verlet.x, verlet.y, radius + 2, radius + 2);
			pg.fill(Color.ACTIVE);
			pg.ellipse(x, y, 3, 3);
			pg.noStroke();
			pg.noFill();
		}
		private void drawInfo(ToxiclibsSupport gfx) {
			PGraphics pg = gfx.getGraphics();
			pg.fill(0xff999999);
			gfx.rect(Rect.fromCenterExtent(new Vec2D(350, verlet.y - 3), new Vec2D(6, 6)));
			pg.fill(Color.BG);
			pg.textAlign(PApplet.CENTER);
//			pg.text("[" + id + "]", 350, y);
			pg.text(id, 350, verlet.y);
			pg.fill(0xff666666);
			pg.textAlign(PApplet.RIGHT);
			pg.text(name, 340, y);
			pg.noFill();
		}
		private void drawDebugInfo(ToxiclibsSupport gfx) {
			PGraphics pg = gfx.getGraphics();
			pg.fill(0xff444444);
			pg.text("s : " + (int) size, x, y);
			pg.text("r : " + (int) radius, x, 70);
			pg.text("x : " + (int) x, x, 80);
			pg.text("vx : " + (int) verlet.x, x, 90);
			pg.text("bx : " + (int) behavior.getAttractor().x, x, 100);
			pg.text("y : " + (int) y, x, 110);
			pg.text("vy : " + (int) verlet.y, x, 120);
			pg.text("by : " + (int) behavior.getAttractor().y, x, 130);
			pg.noFill();
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
/*
	private void drawInfoNew(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		HashMap<String, ArrayList<String>> infoMap = new HashMap<>();
		ArrayList<String> ids = new ArrayList<>();
		ArrayList<String> names = new ArrayList<>();
		ArrayList<String> colors = new ArrayList<>();
		ArrayList<String> sizes = new ArrayList<>();
		ArrayList<String> radii = new ArrayList<>();
		for (Node n : nodes) {
			ids.add(String.valueOf(n.id));
			names.add(n.name);
			colors.add(String.valueOf(n.color));
			sizes.add(String.valueOf(n.size));
			radii.add(String.valueOf(n.getRadius()));
		}
		infoMap.put("id : ", ids);
		infoMap.put("name : ", names);
		infoMap.put("color : ", colors);
		infoMap.put("size : ", sizes);
		infoMap.put("radius : ", radii);

		pg.pushMatrix();
		pg.translate(350, 450);
		pg.fill(0xff666666);
		for (Map.Entry entry : infoMap.entrySet()) {
			pg.translate(0, 10);
			pg.textAlign(PApplet.RIGHT); pg.text(String.valueOf(entry.getKey()), 0, 0);
			pg.textAlign(PApplet.LEFT); pg.text(String.valueOf(entry.getValue()), 0, 0);
		}
		pg.noFill();
		pg.popMatrix();
	}
*/

//	public final Node getNodeForID(int id) { return nodeIndex.get(id); }
//	public final ArrayList<Node> getRelForID(int id) { return relationIndex.get(id); }
//	public void addNode(Node n) {nodes.add(n);}
//	public void addRelation(Relation r) {relations.add(r);}