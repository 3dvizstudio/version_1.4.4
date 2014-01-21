package main;

import controlP5.*;
import org.philhosoft.p8g.svg.P8gGraphicsSVG;
import processing.core.*;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletSpring2D;
import toxi.processing.ToxiclibsSupport;
import util.Color;
import util.RGen;

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
	public static final String dataDirPath = "./data";
	public static final String xmlFilePath = "./data/flowgraph_test_lg.xml";
	public static boolean SHOW_MINDIST, SHOW_ATTRACTORS, SHOW_VOR_VERTS, SHOW_VOR_INFO, SHOW_PHYSINFO;
	public static boolean UPDATE_VORONOI, SHOW_VORONOI, SHOW_TAGS, SHOW_SPRINGS = true, SHOW_NODES = true, SHOW_INFO = true;
	public static float ZOOM = 1, SCALE = 10, DRAG = 0.3f, SPR_SCALE = 1f, SPR_STR = 0.01f, NODE_WGHT = 2, ATTR_RAD = 2, ATTR_STR = -0.9f;
	public static float NODE_STR = -2f, NODE_SCALE = 1, NODE_PAD = 0, OBJ_SIZE = 1, OBJ_HUE = 1, VOR_REFRESH = 1, MIN = 0.1f;
	public static String OBJ_NAME = "new", DRAWMODE = "bezier";
	public static Vec2D MOUSE = new Vec2D();
	private ControlP5 CP5;
	private ToxiclibsSupport GFX;
	public static PSys PSYS;
	public static FSys FSYS;
	public static VSys VSYS;
	static Println console;
	static Textarea myTextarea;
	public static boolean isShiftDown;
	static Group properties, generator, config;
	private Knob radiusSlider, colorSlider;
	private Textfield nameTextfield;
	private ArrayList<FSys.Node> nodes = new ArrayList<>();
	private ArrayList<FSys.Relation> relations = new ArrayList<>();
	static PFont pfont, pfont12;
	static MultiList mainMenu;
	static Accordion accordionLeft;

	public static void main(String[] args) { PApplet.main(new String[]{("main.App")}); }
	public static void __rebelReload() {
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Current relative path is: " + s);
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
//		initGUI();
	}
	public void setup() {
		P5 = this;
		size(1600, 900, P2D);
		frameRate(60);
		smooth(8);
		colorMode(HSB, 360, 100, 100);
		background(Color.BG);
		ellipseMode(RADIUS);
		textAlign(LEFT);
		textSize(10);
		strokeWeight(1);
		noStroke();
		noFill();
		pfont = createFont("SourceCodePro", 10);
		pfont12 = createFont("SourceCodePro", 12);
		GFX = new ToxiclibsSupport(this);
		CP5 = new ControlP5(this);
		PSYS = new PSys();
		FSYS = new FSys();
		VSYS = new VSys(PSYS);
		initGUI();
	}

	public void draw() {
		background(Color.BG);
		PSYS.update();
		VSYS.update();
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
		fill(Color.CP5_BG);
		rect(0, 0, 89, 91);
		noFill();
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
		if (SHOW_VORONOI) VSYS.draw(GFX);
		if (SHOW_PARTICLES) PSYS.draw(GFX);
		if (SHOW_NODES) FSYS.draw(GFX);
	}

	private void createNode(String name) {
		FSys.Node n = new FSys.Node();
		n.name = name;
		n.id = (FSYS.getNodes().size());
		System.out.println(FSYS.getNodes().size());
		n.color = (int) colorSlider.getValue();
		n.size = radiusSlider.getValue();
		n.x = MOUSE.x;
		n.y = MOUSE.y;
		n.build();
		nodes.add(n);
//		FSYS.addNode(n);
		FSYS.updateNodes();
		FSYS.updateSprings();
		marshal();
//		rebuild();
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
		FSYS = new FSys();
		FSYS.setNodes(nodes);
		FSYS.setRelations(relations);
		try {
			JAXBContext context = JAXBContext.newInstance(FSys.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(FSYS, System.out);
			m.marshal(FSYS, new File(xmlFilePath));
		} catch (JAXBException e) { System.out.println("error parsing xml: "); e.printStackTrace(); System.exit(1); }
		FSYS.build();
	}
	private void rebuild() {
		FSYS.setNodes(nodes);
		FSYS.setRelations(relations);
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
				accordionLeft.open(2);
			} else {
				radiusSlider.hide();
				colorSlider.hide();
				nameTextfield.hide();
				accordionLeft.close(2);
			}
		}
	}
	public void mouseDragged() {
		Vec2D mousePos = new Vec2D(mouseX, mouseY);
		if (mouseButton == RIGHT) {
			if (FSYS.hasActiveNode()) { FSYS.moveActiveNode(mousePos); }
		}
	}
	public void mouseReleased() {}
	public void keyPressed() {
		if (key == CODED && keyCode == SHIFT) { isShiftDown = true; }
		switch (key) {
			case 'a': System.out.println("a"); createNode(OBJ_NAME); break;
			case 'f': System.out.println("f");
				if (FSYS.getSelectedNodes().size() >= 2) {
					FSys.Node na = FSYS.getSelectedNodes().get(0);
					FSys.Node nb = FSYS.getSelectedNodes().get(1);
					createSpring(FSYS.getSelectedNodes().get(0), FSYS.getSelectedNodes().get(1));
					FSYS.getSelectedNodes().clear();
					FSYS.getSelectedNodes().add(na);
					FSYS.getSelectedNodes().add(nb);
					FSYS.setActiveNode(na);
				} break;
			case 'q': System.out.println("q");
				if (FSYS.hasActiveNode()) {
					RGen rgen = new RGen(FSYS.getActiveNode(), nodes.size(), 3, 50, true);
					nodes.addAll(rgen.getNodes()); relations.addAll(rgen.getRelations());
					marshal();
				} break;
			case 'w': System.out.println("W");
				if (FSYS.hasActiveNode()) {
					RGen rgen = new RGen(FSYS.getActiveNode(), nodes.size(), 3, 50, false);
					nodes.addAll(rgen.getNodes()); relations.addAll(rgen.getRelations());
					marshal();
				} break;
			case 'x': if (FSYS.hasActiveNode()) { FSYS.getNodes().remove(FSYS.getActiveNode()); }
			case '1': System.out.println("1"); VSYS.setDrawMode("poly"); break;
			case '2': System.out.println("2"); VSYS.setDrawMode("bezier"); break;
			case '3': System.out.println("3"); VSYS.setDrawMode("verts"); break;
			case '4': System.out.println("4"); VSYS.setDrawMode("info"); break;
			case 'u': FSYS.update(); break;
		}
	}
	public void keyReleased() { if (key == CODED && keyCode == SHIFT) { isShiftDown = false; } }

	private void initGUI() {
		CP5.enableShortcuts();
		CP5.setAutoDraw(false);
		CP5.setFont(pfont, 10);
		CP5.setAutoSpacing(4, 8);
		CP5.setColorBackground(Color.CP5_BG).setColorForeground(Color.CP5_FG).setColorActive(Color.CP5_ACT);
		CP5.setColorCaptionLabel(Color.CP5_CAP).setColorValueLabel(Color.CP5_VAL);
		config = CP5.addGroup("VERLET PHYSICS SETTINGS").setBackgroundHeight(350).setBarHeight(32).setWidth(219);
		generator = CP5.addGroup("RECURSIVE GRAPH GENERATOR").setBackgroundHeight(140).setBarHeight(32).setWidth(219);
		properties = CP5.addGroup("OBJECT_PROPERTIES").setBackgroundHeight(200).setBarHeight(32).setWidth(219);
		initGUI_left();
		initGUI_right();
		initGUI_menu();
		initGUI_styles();
		accordionLeft = CP5.addAccordion("accL").setPosition(0, 92).setWidth(220);
		accordionLeft.addItem(config).addItem(generator).addItem(properties);
		accordionLeft.setCollapseMode(Accordion.MULTI);
	}

	private void initGUI_menu() {
		mainMenu = CP5.addMultiList("myList", 90, 0, 130, 24);
		MultiListButton file;
		file = mainMenu.add("File", 1); file.setWidth(130); file.setHeight(20);
		file.add("file_quit", 11).setCaptionLabel("Quit");
		file.add("file_open", 12).setCaptionLabel("Open XML");
		file.add("file_save", 13).setCaptionLabel("Save XML");
		file.add("file_print", 14).setCaptionLabel("Print SVG");
		file.add("file_loadDef", 15).setCaptionLabel("Load Defaults");
		file.add("file_saveDef", 16).setCaptionLabel("Save Defaults");
		MultiListButton view;
		view = mainMenu.add("View", 2); view.setWidth(130); view.setHeight(20);
		view.add("view_nodes", 21).setCaptionLabel("Show Nodes");
		view.add("view_particles", 22).setCaptionLabel("Show Particles");
		view.add("view_springs", 23).setCaptionLabel("Show Springs");
		view.add("view_voronoi", 24).setCaptionLabel("Show Voronoi");
		view.add("view_nodeInfo", 25).setCaptionLabel("Show Info");
		view.add("view_particleInfo", 26).setCaptionLabel("Show Particles");
		view.add("view_vorInfo", 27).setCaptionLabel("Show Vor Info");
		MultiListButton run;
		run = mainMenu.add("Run", 3); run.setWidth(130); run.setHeight(20);
		run.add("run_physics", 31).setCaptionLabel("Run Physics");
		run.add("run_voronoi", 32).setCaptionLabel("Run Voronoi");
		run.add("run_updateVals", 33).setCaptionLabel("Update Values");
		MultiListButton edit;
		edit = mainMenu.add("Edit", 4); edit.setWidth(130); edit.setHeight(20);
		edit.add("edit_addMinDist", 41).setCaptionLabel("Add MinDist");
		edit.add("edit_rebuildMinD", 42).setCaptionLabel("Rebuild MinDist");
		edit.add("edit_clearMinD", 43).setCaptionLabel("Clear MinDist");
		edit.add("edit_clearSpr", 44).setCaptionLabel("Clear Springs");
		edit.add("edit_clearAll", 45).setCaptionLabel("Clear All");
	}

	private void initGUI_left() {
		CP5.begin(10, 32);
		radiusSlider = CP5.addKnob("setSize").setRange(0, 500).setValue(50).setPosition(30, 20);
		radiusSlider.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				FSYS.getActiveNode().setSize(e.getController().getValue());
				if (e.getController().isMousePressed()) {
					for (FSys.Node n : FSYS.getSelectedNodes()) { n.setSize(e.getController().getValue()); }
				}
//				FSYS.update();
				FSYS.updateNodes();
				FSYS.updateSprings();
			}
		});
		radiusSlider.hide();
		colorSlider = CP5.addKnob("setColor").setRange(0, 360).setValue(180).setPosition(120, 20);
		colorSlider.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				FSYS.getActiveNode().setColor((int) e.getController().getValue());
				if (e.getController().isMousePressed()) {
					for (FSys.Node n : FSYS.getSelectedNodes()) { n.setColor((int) e.getController().getValue()); }
				}
			}
		});

		colorSlider.hide();
		nameTextfield = CP5.addTextfield("setName").setText("untitled").setPosition(20, 120);
		nameTextfield.setCaptionLabel("Unique Datablock ID Name");
		nameTextfield.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				FSYS.getActiveNode().setName(e.getController().getStringValue());
				if (e.getController().isMousePressed()) {
					for (FSys.Node n : FSYS.getSelectedNodes()) { n.setName(e.getController().getStringValue()); }
				}
			}
		});
		nameTextfield.hide();
		CP5.end();
	}
	private void initGUI_right() {
		CP5.begin(10, 10);
		CP5.addSlider("world_scale").setRange(1, 20).setValue(5).linebreak();
		CP5.addSlider("world_zoom").setRange(1, 2).setValue(1).linebreak();
		CP5.addSlider("world_drag").setRange(0.1f, 1).setValue(.3f).linebreak();
		CP5.addSlider("node_scale").setRange(0.1f, 2).setValue(1).setNumberOfTickMarks(20).linebreak();
		CP5.addSlider("node_strength").setRange(-4, -.01f).setValue(-.01f).linebreak();
		CP5.addSlider("node_padding").setRange(0.01f, 9).setValue(.01f).linebreak();
		CP5.addSlider("node_weight").setRange(0.1f, 10).setValue(2).linebreak();
		CP5.addSlider("spring_scale").setRange(0.1f, 2).setValue(1).setNumberOfTickMarks(20).linebreak();
		CP5.addSlider("spring_strength").setRange(0.001f, 0.05f).setValue(.01f).linebreak();
		CP5.addSlider("attr_strength").setRange(-2f, 2).setValue(-.01f).linebreak();
		CP5.addSlider("attr_radius").setRange(0.1f, 10).setValue(50).linebreak();
		CP5.end();
		CP5.begin(10, 10);
		CP5.addNumberbox("ITER_A").setPosition(10, 14).linebreak();
		CP5.addNumberbox("ITER_B").setPosition(10, 38).linebreak();
		CP5.addNumberbox("ITER_C").setPosition(10, 62).linebreak();
		CP5.addNumberbox("ITER_D").setPosition(10, 86).linebreak();
		CP5.addNumberbox("ITER_E").setPosition(10, 110).linebreak();
		CP5.end();
	}
	private void initGUI_styles() {
		for (Button b : CP5.getAll(Button.class)) {
			b.setSize(130, 22);
			b.setColorBackground(Color.CP5_BG);
			b.setColorActive(Color.CP5_ACT);
			b.setColorForeground(Color.CP5_FG);
			b.getCaptionLabel().setColor(Color.CP5_CAP);
			b.getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER).setFont(pfont);
		}
		for (Toggle t : CP5.getAll(Toggle.class)) {
			t.setSize(90, 16);
			t.setColorBackground(0xff444444);
			t.setColorActive(Color.CP5_FG);
			t.setColorForeground(Color.CP5_FG);
			t.getCaptionLabel().setColor(Color.CP5_CAP);
			t.getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER).getStyle();
		} for (Bang b : CP5.getAll(Bang.class)) {
			b.setSize(90, 24);
			b.setColorBackground(Color.CP5_FG);
			b.setColorActive(0xff555555);
			b.setColorForeground(Color.BG);
			b.getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER).setFont(pfont12);
			b.getCaptionLabel().setColor(Color.BLACK);
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
				case "file_quit": System.out.println("[quit]"); exit(); break;
				case "file_open": unmarshal(); break;
				case "file_save": marshal(); break;
				case "file_print": beginRecord(P8gGraphicsSVG.SVG, "./out/svg/print-###.svg"); RECORDING = true; break;
				case "file_loadDef": CP5.loadProperties((xmlFilePath + "defaults.ser")); break;
				case "file_saveDef": CP5.saveProperties((xmlFilePath + "defaults.ser")); break;

				case "view_nodes": SHOW_NODES = !SHOW_NODES; break;
				case "view_particles": SHOW_PARTICLES = !SHOW_PARTICLES; break;
				case "view_springs": SHOW_SPRINGS = !SHOW_SPRINGS; break;
				case "view_voronoi": SHOW_VORONOI = !SHOW_VORONOI; break;
				case "view_nodeInfo": SHOW_INFO = !SHOW_INFO; break;
				case "view_particleInfo": SHOW_PHYSINFO = !SHOW_PHYSINFO; break;
				case "view_vorInfo": SHOW_VOR_INFO = !SHOW_VOR_INFO; break;

				case "run_physics": UPDATE_PHYSICS = !UPDATE_PHYSICS; break;
				case "run_voronoi": UPDATE_VORONOI = !UPDATE_VORONOI; break;
				case "run_updateVals": UPDATE_VALUES = !UPDATE_VALUES; break;

				case "edit_addMinDist": PSYS.addMinDistSprings(); break;
				case "edit_rebuildMinD": PSYS.rebuildMinDistSprings(); break;
				case "edit_clearMinD": PSYS.clearMinDistSprings(); break;
				case "edit_clearSpr": PSYS.clearSprings(); break;
				case "edit_clearAll": PSYS.clear(); FSYS.clear(); break;

				case "world_scale": SCALE = theEvent.getController().getValue();
//					FSYS.update();
					FSYS.updateNodes();
					FSYS.updateSprings();
					break;
				case "world_zoom": ZOOM = theEvent.getController().getValue(); break;
				case "world_drag": DRAG = theEvent.getController().getValue(); break;
				case "node_scale": NODE_SCALE = theEvent.getController().getValue(); FSYS.updateNodes(); FSYS.updateSprings(); break;
				case "node_strength": NODE_STR = theEvent.getController().getValue(); FSYS.updateNodes(); break;
				case "node_padding": NODE_PAD = theEvent.getController().getValue(); FSYS.updateNodes(); FSYS.updateSprings(); break;
				case "node_weight": NODE_WGHT = theEvent.getController().getValue(); FSYS.updateNodes(); break;
				case "spring_scale": SPR_SCALE = theEvent.getController().getValue(); FSYS.updateSprings(); break;
				case "spring_strength": SPR_STR = theEvent.getController().getValue(); FSYS.updateSprings(); break;
				case "attr_strength": ATTR_STR = theEvent.getController().getValue(); break;
				case "attr_radius": ATTR_RAD = theEvent.getController().getValue(); break;
			}
		}
	}
}
/*
FSYS.update();
				FSYS.updateNodes();
				FSYS.updateSprings();
*/

/*
	private void initGUI_console() {
		myTextarea = CP5.addTextarea("txt").setPosition(1200, 50).setSize(400, 800);
		console = CP5.addConsole(myTextarea);
	}
*/

/*	private static void initGUI_sidebar() {
		CP5.begin(220, 0);
		CP5.addButton("quit").setId(1).linebreak();
		CP5.addBang("FileIO").linebreak();
		CP5.addButton("unmarshal").setCaptionLabel("Open XML").linebreak();
		CP5.addButton("marshal").setCaptionLabel("Save XML").linebreak();
		CP5.addButton("save_svg").setCaptionLabel("Print SVG").linebreak();
		CP5.addButton("load_def").setCaptionLabel("Load Def.").linebreak();
		CP5.addButton("save_def").setCaptionLabel("Save Def.").linebreak();
		CP5.addBang("DisplayPhys").linebreak();
		CP5.addToggle("SHOW_INFO").setCaptionLabel("Info").linebreak();
		CP5.addToggle("SHOW_NODES").setCaptionLabel("NODES").linebreak();
		CP5.addToggle("SHOW_PARTICLES").setCaptionLabel("PARTICLES").linebreak();
		CP5.addToggle("SHOW_SPRINGS").setCaptionLabel("SPRINGS").linebreak();
		CP5.addToggle("SHOW_MINDIST").setCaptionLabel("MinD Spr").linebreak();
		CP5.addBang("DisplayVor").linebreak();
		CP5.addToggle("SHOW_VORONOI").setCaptionLabel("VORONOI").linebreak();
		CP5.addToggle("SHOW_VOR_VERTS").setCaptionLabel("VOR.VERTS").linebreak();
		CP5.addToggle("SHOW_VOR_INFO").setCaptionLabel("VOR.INFO").linebreak();
		CP5.addToggle("SHOW_VOIDS").setCaptionLabel("VOIDS").linebreak();
		CP5.addBang("UPDATE").linebreak();
		CP5.addToggle("UPDATE_VORONOI").setCaptionLabel("UPDATE VOR").linebreak();
		CP5.addToggle("UPDATE_VALUES").setCaptionLabel("UPDATE VALS").linebreak();
		CP5.addToggle("UPDATE_PHYSICS").setCaptionLabel("UPDATE PHYS").linebreak();
		CP5.addBang("EDIT").linebreak();
		CP5.addButton("add_mindist").setCaptionLabel("Add MinD").linebreak();
		CP5.addButton("clear_mindist").setCaptionLabel("Clear MinD").linebreak();
		CP5.addButton("rebuild_mindist").setCaptionLabel("Rebuild MinD").linebreak();
		CP5.addButton("clear_springs").setCaptionLabel("Clear Springs").linebreak();
		CP5.addButton("clear_all").setCaptionLabel("Clear All").linebreak();
		CP5.end();
	}*/
/*	private void marshalOld() {
		System.out.println("MarshalOld-------------------------------------");
		FSys flowgraph = new FSys();
		flowgraph.setNodes(nodes);
		flowgraph.setRelations(relations);
		try {
			JAXBContext context = JAXBContext.newInstance(FSys.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(flowgraph, System.out);
			m.marshal(flowgraph, new File(xmlFilePath));
		} catch (JAXBException e) { System.out.println("error parsing xml: "); e.printStackTrace(); System.exit(1); }
		FSYS.build();
	}*/
/*				for (FSys.Node n : FSYS.getRelForID(FSYS.getActiveNode().id)) {
					System.out.println(n.id);
					System.out.println(FSYS.getRelations().get(n.id).toString());
					for (FSys.Relation r : FSYS.getRelations()) {
						if (r.from == FSYS.getActiveNode().id) {
							System.out.println("index: " + FSYS.getRelations().indexOf(r));
							if (r.to == n.id) {
								System.out.println("hoopda" + FSYS.getRelations().indexOf(r)); break;
							}
						}
					}
//					else if (r.to == FSYS.getActiveNode().id) { FSYS.getRelations().remove(r); }
				}*/
//				}