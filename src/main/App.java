package main;

import controlP5.*;
import org.philhosoft.p8g.svg.P8gGraphicsSVG;
import processing.core.PApplet;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletSpring2D;
import toxi.processing.ToxiclibsSupport;
import util.Color;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class App extends PApplet {
	public static PApplet P5;
	public static final DecimalFormat DF3 = new DecimalFormat("#.###");
	public static boolean RECORDING = false, UPDATE_PHYSICS = true, UPDATE_VALUES = true, SHOW_PARTICLES = true;
	public static final String xmlFilePath = "F:\\Java\\Projects\\thesis_2014\\version_1.4.2\\data\\flowgraph_test_lg.xml";
	public static boolean SHOW_MINDIST, SHOW_ATTRACTORS, SHOW_VOR_VERTS, SHOW_VOR_INFO, SHOW_VOIDS;
	public static boolean UPDATE_VORONOI, SHOW_VORONOI, SHOW_TAGS, SHOW_SPRINGS = true, SHOW_NODES = true, SHOW_INFO = true;
	public static float ZOOM = 1, SCALE = 10, DRAG = 0.3f, SPR_SCALE = 1f, SPR_STR = 0.01f, NODE_WGHT = 2, ATTR_RAD = 2, ATTR_STR = -0.9f;
	public static float NODE_STR = -2f, NODE_SCALE = 1, NODE_PAD = 0, OBJ_SIZE = 1, OBJ_HUE = 1, VOR_REFRESH = 1, MIN = 0.1f;
	public static String OBJ_NAME = "new", DRAWMODE = "bezier";
	public static Vec2D MOUSE = new Vec2D();
	private static ControlP5 CP5;
	private ToxiclibsSupport GFX;
	public static PSys PSYS;
	public static FSys FSYS;
	static Println console;
	static Textarea myTextarea;
	public static boolean isShiftDown;
	static Group properties, generator, config;
	private Knob radiusSlider, colorSlider;
	private static Textfield nameTextfield;
	private static ArrayList<FSys.Node> nodes = new ArrayList<>();
	private static ArrayList<FSys.Relation> relations = new ArrayList<>();

	public static void main(String[] args) { PApplet.main(new String[]{("main.App")}); }
	public static void __rebelReload() {
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Current relative path is: " + s);
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
	}

	public void setup() {
		size(1600, 900);
		frameRate(60);
		smooth(4);
		colorMode(HSB, 360, 100, 100);
		ellipseMode(RADIUS);
		textAlign(LEFT);
		textSize(10);
		strokeWeight(1);
		noStroke();
		noFill();
		GFX = new ToxiclibsSupport(this);
		CP5 = new ControlP5(this);
		PSYS = new PSys();
		FSYS = new FSys();
		initGUI();
	}
	public void draw() {
		background(Color.BG);
		PSYS.update();
		if ((UPDATE_VALUES) && (SCALE != 0)) { FSYS.update();}
		if (RECORDING) { RECORDING = false; endRecord(); System.out.println("SVG EXPORTED SUCCESSFULLY"); }
		MOUSE.set(mouseX, mouseY);
		pushMatrix();
		translate(-((ZOOM * width) - width) / 2, -((ZOOM * height) - height) / 2);
		scale(ZOOM);
		draw_shapes2D();
		popMatrix();
		draw_GUI();
	}
	private void draw_GUI() {
		CP5.draw();
		float totalSize = 0;
		for (FSys.Node n : nodes) { totalSize += n.size; }
		HashMap<String, String> infoMap = new HashMap<>();
		infoMap.put("Mouse : ", String.valueOf((int) MOUSE.x + ":" + (int) MOUSE.y));
		infoMap.put("AreaTot : ", DF3.format(totalSize));
		infoMap.put("Springs : ", String.valueOf(PSYS.getPhysics().springs.size()));
		infoMap.put("Particles : ", String.valueOf(PSYS.getPhysics().particles.size()));
		infoMap.put("Behaviors : ", String.valueOf(PSYS.getPhysics().behaviors.size()));
		infoMap.put("Drag : ", DF3.format(PSYS.getPhysics().getDrag()));

		pushMatrix();
		translate(350, 450);
		fill(0xff666666);
		for (Map.Entry entry : infoMap.entrySet()) {
			translate(0, 10);
			textAlign(RIGHT); text(String.valueOf(entry.getKey()), 0, 0);
			textAlign(LEFT); text(String.valueOf(entry.getValue()), 0, 0);
		}
		noFill();
		popMatrix();
	}
	private void draw_shapes2D() {
		if (SHOW_PARTICLES) PSYS.draw(GFX);
		if (SHOW_NODES) FSYS.draw(GFX);
	}

	private void createNode(String name, Vec2D pos, float size, int color, int id) {
		FSys.Node n = new FSys.Node();
		n.name = name;
		n.id = nodes.size();
		n.color = (int) colorSlider.getValue();
		n.size = radiusSlider.getValue();
		n.x = MOUSE.x;
		n.y = MOUSE.y;
		n.build();
		nodes.add(n);
		marshal();
	}
	private void createSpring(FSys.Node p1, FSys.Node p2) {
		FSys.Relation r = new FSys.Relation();
		r.setFrom(p1.id);
		r.setTo(p2.id);
		float len = p1.getRadius() + p2.getRadius() + 5;
		VerletSpring2D s = new VerletSpring2D(p1.verlet, p2.verlet, len, 0.01f);
		relations.add(r);
		marshal();
	}
	private void unmarshal() {
		System.out.println("Unmarshal-----------------------------------");
		try {
			JAXBContext context = JAXBContext.newInstance(FSys.class);
			FSYS = (FSys) context.createUnmarshaller().unmarshal(createInput(xmlFilePath));
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(FSYS, System.out);
		} catch (JAXBException e) { System.out.println("error parsing xml: "); e.printStackTrace(); System.exit(1); }
		FSYS.build();
	}
	private void marshal() {
		System.out.println("Marshal-------------------------------------");
		FSys flowgraph = new FSys();
		flowgraph.setNodes(nodes);
		flowgraph.setRelations(relations);
		try {
			JAXBContext context = JAXBContext.newInstance(FSys.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(flowgraph, System.out);
			m.marshal(flowgraph, new File(App.xmlFilePath));
		} catch (JAXBException e) { System.out.println("error parsing xml: "); e.printStackTrace(); System.exit(1); }
		FSYS.build();
	}

	public void mouseMoved() { }
	public void mousePressed() {
		Vec2D mousePos = new Vec2D(mouseX, mouseY);
		if (mouseButton == RIGHT) {
			FSYS.selectNodeNearPosition(mousePos);
			if (FSYS.hasActiveNode()) {
				radiusSlider.setValue(FSYS.getActiveNode().size);
				colorSlider.setValue(FSYS.getActiveNode().color);
				nameTextfield.setValue(FSYS.getActiveNode().name);
				radiusSlider.show();
				colorSlider.show();
				nameTextfield.show();
			} else {
				radiusSlider.hide();
				colorSlider.hide();
				nameTextfield.hide();
			}
		}
	}
	public void mouseDragged() {
		Vec2D mousePos = new Vec2D(mouseX, mouseY);
		if (mouseButton == RIGHT) {
			if (FSYS.hasActiveNode()) { FSYS.moveActiveNode(mousePos); }
		}
	}
	public void mouseReleased() {
	}
	public void keyPressed() {
		if (key == CODED && keyCode == SHIFT) { isShiftDown = true; }
		switch (key) {
			case 'a':
				createNode(OBJ_NAME, MOUSE, OBJ_SIZE, (int) OBJ_HUE, nodes.size());
				break;
			case 'p':
				marshal();
				break;
			case 'r':
				if (FSYS.getSelectedNodes().size() >= 2) { createSpring(FSYS.getSelectedNodes().get(0), FSYS.getSelectedNodes().get(1)); FSYS.getSelectedNodes().clear(); }
				break;
			case 'y':
				PSYS.clearSprings();
				break;
			case 'm':
				if (FSYS.hasActiveNode()) { FSYS.getActiveNode().verlet.set(MOUSE); }
				break;
			case 'u':
				for (FSys.Node n : FSYS.getNodes()) { n.verlet.set(n.x, n.y); }
				break;
		}
	}
	public void keyReleased() { if (key == CODED && keyCode == SHIFT) { isShiftDown = false; } }

	private void initGUI() {
		CP5.enableShortcuts();
		CP5.setAutoDraw(false);
		CP5.setAutoSpacing(4, 8);
		CP5.setColorBackground(Color.CP5_BG).setColorForeground(Color.CP5_FG).setColorActive(Color.CP5_ACT);
		CP5.setColorCaptionLabel(Color.CP5_CAP).setColorValueLabel(Color.CP5_VAL);
		config = CP5.addGroup("VERLET PHYSICS SETTINGS").setBackgroundHeight(350).setBarHeight(32).setWidth(219);
		generator = CP5.addGroup("RECURSIVE GRAPH GENERATOR").setBackgroundHeight(140).setBarHeight(32).setWidth(219);
		properties = CP5.addGroup("OBJECT_PROPERTIES").setBackgroundHeight(200).setBarHeight(32).setWidth(219);
		initGUI_sidebar();
		initGUI_left();
		initGUI_right();
//		initGUI_console();
		initGUI_styles();
		Accordion accordionLeft = CP5.addAccordion("accL").setPosition(80, 0).setWidth(219);
		accordionLeft.addItem(config).addItem(generator).addItem(properties);
		accordionLeft.setCollapseMode(Accordion.MULTI); accordionLeft.open(2);
	}
	private void initGUI_sidebar() {
		CP5.begin(0, 0);
		CP5.addButton("quit").setId(1).linebreak();
		CP5.addBang("FileIO").linebreak();
		CP5.addButton("load_xml").setCaptionLabel("Open XML").linebreak();
		CP5.addButton("save_svg").setCaptionLabel("Print SVG").linebreak();
		CP5.addButton("load_def").setCaptionLabel("Load Defaults").linebreak();
		CP5.addButton("save_def").setCaptionLabel("Save as Default").linebreak();
		CP5.addBang("PhysOperations").linebreak();
		CP5.addButton("add_mindist").setCaptionLabel("Add MinDist").linebreak();
		CP5.addButton("clear_phys").setCaptionLabel("Clear All").linebreak();
		CP5.addBang("DisplayPhys").linebreak();
		CP5.addToggle("SHOW_INFO").setCaptionLabel("Info").linebreak();
		CP5.addToggle("SHOW_NODES").setCaptionLabel("NODES").linebreak();
		CP5.addToggle("SHOW_PARTICLES").setCaptionLabel("PARTICLES").linebreak();
		CP5.addToggle("SHOW_SPRINGS").setCaptionLabel("SPRINGS").linebreak();
		CP5.addToggle("SHOW_MINDIST").setCaptionLabel("MIN.DIST").linebreak();
		CP5.addBang("DisplayVor").linebreak();
		CP5.addToggle("SHOW_VORONOI").setCaptionLabel("VORONOI").linebreak();
		CP5.addToggle("SHOW_VOR_VERTS").setCaptionLabel("VOR.VERTS").linebreak();
		CP5.addToggle("SHOW_VOR_INFO").setCaptionLabel("VOR.INFO").linebreak();
		CP5.addToggle("SHOW_VOIDS").setCaptionLabel("VOIDS").linebreak();
		CP5.addBang("UPDATE").linebreak();
		CP5.addToggle("UPDATE_VALUES").setCaptionLabel("VALUES").linebreak();
		CP5.addToggle("UPDATE_PHYSICS").setCaptionLabel("PHYSICS").linebreak();
		CP5.addToggle("UPDATE_VORONOI").setCaptionLabel("VORONOI").linebreak();
		CP5.end();
	}
	private void initGUI_left() {
		CP5.begin(10, 32);
		radiusSlider = CP5.addKnob("setSize").setRange(0, 500).setDefaultValue(50).setPosition(30, 20);
		radiusSlider.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				FSYS.getActiveNode().setSize(e.getController().getValue()); FSYS.update();
			}
		});
		radiusSlider.hide();
		colorSlider = CP5.addKnob("setColor").setRange(0, 360).setDefaultValue(180).setPosition(120, 20);
		colorSlider.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				FSYS.getActiveNode().setColor((int) e.getController().getValue());
			}
		});
		colorSlider.hide();
		nameTextfield = CP5.addTextfield("setName").setText("untitled").setPosition(20, 120);
		nameTextfield.setCaptionLabel("Unique Datablock ID Name");
		nameTextfield.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				FSYS.getActiveNode().setName(e.getController().getStringValue());
			}
		});
		nameTextfield.hide();
		CP5.end();
	}
	private void initGUI_right() {
		CP5.begin(10, 10);
		CP5.addSlider("SCALE").setRange(1, 20).setNumberOfTickMarks(20).linebreak();
		CP5.addSlider("ZOOM").setRange(1, 20).setNumberOfTickMarks(20).linebreak();
		CP5.addSlider("DRAG").setRange(0.1f, 1).linebreak();
		CP5.addSlider("NODE_SCALE").setRange(0.1f, 2).setNumberOfTickMarks(20).linebreak();
		CP5.addSlider("NODE_STR").setRange(-4, -.01f).linebreak();
		CP5.addSlider("NODE_PAD").setRange(0.1f, 9).linebreak();
		CP5.addSlider("SPR_SCALE").setRange(0.1f, 2).setNumberOfTickMarks(20).linebreak();
		CP5.addSlider("SPR_STR").setRange(0.001f, 0.05f).linebreak();
		CP5.addSlider("ATTR_STR").setRange(-2f, 2).linebreak();
		CP5.addSlider("ATTR_RAD").setRange(0.1f, 10).linebreak();
		CP5.addSlider("NODE_WGHT").setRange(0.1f, 10).linebreak();
		CP5.end();
		CP5.begin(10, 10);
		CP5.addNumberbox("ITER_A").setPosition(10, 14).linebreak();
		CP5.addNumberbox("ITER_B").setPosition(10, 38).linebreak();
		CP5.addNumberbox("ITER_C").setPosition(10, 62).linebreak();
		CP5.addNumberbox("ITER_D").setPosition(10, 86).linebreak();
		CP5.addNumberbox("ITER_E").setPosition(10, 110).linebreak();
		CP5.end();
	}
	private void initGUI_console() {
		myTextarea = CP5.addTextarea("txt").setPosition(1200, 50).setSize(400, 800);
		console = CP5.addConsole(myTextarea);
	}
	private void initGUI_styles() {
		for (Button b : CP5.getAll(Button.class)) {
			b.setSize(80, 22);
			b.setColorBackground(Color.CP5_BG);
			b.setColorActive(0xff999999);
			b.setColorForeground(0xff666666);
			b.getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER).getStyle();
			b.getCaptionLabel().setColor(Color.CP5_CAP);
		}
		for (Toggle t : CP5.getAll(Toggle.class)) {
			t.setSize(80, 16);
			t.setColorBackground(Color.CP5_BG);
			t.setColorActive(0xff555555);
			t.setColorForeground(0xff888888);
			t.getCaptionLabel().setColor(Color.CP5_CAP);
			t.getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER).getStyle();
		} for (Bang b : CP5.getAll(Bang.class)) {
			b.setSize(80, 24);
			b.setColorBackground(Color.CP5_FG);
			b.setColorActive(0xff555555);
			b.setColorForeground(Color.BG);
			b.getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER).getStyle();
			b.getCaptionLabel().setColor(Color.CP5_FG);
		} for (Slider s : CP5.getAll(Slider.class)) {
			s.setSize(170, 16);
			s.setGroup(config);
			s.showTickMarks(false).setHandleSize(8);
			s.setSliderMode(Slider.FLEXIBLE);
			s.setColorForeground(Color.CP5_FG).setColorActive(Color.CP5_CAP);
			s.getValueLabel().align(ControlP5.RIGHT_OUTSIDE, ControlP5.CENTER).getStyle().setPaddingLeft(4);
			s.getCaptionLabel().align(ControlP5.RIGHT, ControlP5.CENTER).getStyle().setPaddingRight(4);
		} for (Numberbox b : CP5.getAll(Numberbox.class)) {
			b.setSize(200, 16).setRange(0, 10).setDirection(Controller.HORIZONTAL).setMultiplier(0.05f).setDecimalPrecision(0);
			b.setGroup(generator);
			b.getCaptionLabel().align(ControlP5.RIGHT, ControlP5.CENTER);
			b.getValueLabel().align(ControlP5.LEFT, ControlP5.CENTER);
		} for (Knob k : CP5.getAll(Knob.class)) {
			k.setRadius(30);
			k.setDragDirection(Knob.HORIZONTAL);
			k.setGroup(properties);
		} for (Textfield t : CP5.getAll(Textfield.class)) {
			t.setSize(180, 32);
			t.setAutoClear(false);
			t.setColorForeground(Color.CP5_ACT);
			t.setColorBackground(Color.BG_MENUS);
			t.setGroup(properties);
			t.getValueLabel().setPaddingX(6);
			t.getCaptionLabel().align(ControlP5.CENTER, ControlP5.BOTTOM_OUTSIDE).getStyle().setPaddingTop(4);
		} for (Textarea t : CP5.getAll(Textarea.class)) {
			t.setLineHeight(14);
			t.setColor(0xff333333).setColorBackground(Color.CP5_BG).setColorForeground(Color.CP5_FG);
			t.getCaptionLabel().align(ControlP5.CENTER, ControlP5.BOTTOM_OUTSIDE).getStyle().setPaddingTop(4);
			t.disableColorBackground();
		} for (Group g : CP5.getAll(Group.class)) {
			g.setBackgroundColor(0xff222222);
			g.getCaptionLabel().align(ControlP5.LEFT, ControlP5.CENTER).getStyle().setPaddingLeft(4);
		}
	}
	public void controlEvent(ControlEvent theEvent) {
		if (!theEvent.isGroup()) {
			switch (theEvent.getController().getName()) {
				case "quit":
					System.out.println("[quit]"); exit(); break;
				case "load_xml":
					System.out.println("[load_xml]"); unmarshal(); break;
				case "save_svg":
					beginRecord(P8gGraphicsSVG.SVG, "./out/svg/print-###.svg"); RECORDING = true; break;
				case "clear_phys":
					PSYS.clear(); FSYS.clear(); break;
				case "load_conf":
					CP5.loadProperties(("C:\\Users\\admin\\Projects\\IdeaProjects\\thesis\\version_1\\lib\\config\\config.ser")); break;
				case "load_def":
					CP5.loadProperties(("C:\\Users\\admin\\Projects\\IdeaProjects\\thesis\\version_1\\lib\\config\\defaults.ser")); break;
				case "save_conf":
					CP5.saveProperties(("C:\\Users\\admin\\Projects\\IdeaProjects\\thesis\\version_1\\lib\\config\\config.ser")); break;
				case "save_def":
					CP5.saveProperties(("C:\\Users\\admin\\Projects\\IdeaProjects\\thesis\\version_1\\lib\\config\\defaults.ser")); break;
				case "add_mindist":
					break;
			}
		}
	}
}