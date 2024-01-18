import model3D.*;
import rasterizer.ImageBuffer;
import rasterizer.line.LineRasterizer;
import renderer.Renderer3D;
import transforms.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;


public class Canvas3D {
    private JPanel panel;
    private ImageBuffer img;
    private Renderer3D renderer;

    private Scene scene;
    private Object3D cube, pyramid, prism;
    private Object3D Ox, Oy, Oz;
    private int activeObj;
    private Vec3D viewPos;
    private int x, y;
    private Camera camera;
    private Mat4 chosenProjection;
    private Mat4PerspRH perspectiveMatrix;
    private Mat4OrthoRH orthMatrix;



    private Color red, green, blue, greenish, grey, yellow, azure;

    public Canvas3D(int width, int height) {
        JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.setTitle("UHK FIM PGRF : " + this.getClass().getName());
        frame.setResizable(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        panel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                present(g);
            }
        };
        panel.setPreferredSize(new Dimension(width, height));

        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        panel.requestFocus();
        panel.requestFocusInWindow();

        img = new ImageBuffer(width, height);
        setupCanvas();
        clear();
        initScene();
        drawScene();

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                x = e.getX();
                y = e.getY();
            }
        });
        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                camera = camera.addAzimuth( (double) ( x - e.getX() )/200)
                         .addZenith( (double) ( y- e.getY())/200 );
                x = e.getX();
                y = e.getY();
                drawScene();
            }
        });
        panel.addMouseWheelListener(e -> {
            if(e.getWheelRotation() < 0) {// moved up
                camera = camera.backward(0.2);
            }
            else {
                camera = camera.forward(0.2);
            }
            drawScene();
        });
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Object3D current = scene.getObjects().get(activeObj);
                //translations
                if(e.getKeyCode() == KeyEvent.VK_LEFT)
                    current.setModelMat(current.getModelMat().mul(new Mat4Transl(0.05, 0, 0)));
                if(e.getKeyCode() == KeyEvent.VK_RIGHT)
                    current.setModelMat(current.getModelMat().mul(new Mat4Transl(-0.05, 0, 0)));
                if(e.getKeyCode() == KeyEvent.VK_UP)
                    current.setModelMat(current.getModelMat().mul(new Mat4Transl(0, 0, 0.05)));
                if(e.getKeyCode() == KeyEvent.VK_DOWN)
                    current.setModelMat(current.getModelMat().mul(new Mat4Transl(0, 0, -0.05)));
                if(e.getKeyCode() ==KeyEvent.VK_M)
                    current.setModelMat(current.getModelMat().mul(new Mat4Transl(0, 0.05, 0)));
                if(e.getKeyCode() ==KeyEvent.VK_L)
                    current.setModelMat(current.getModelMat().mul(new Mat4Transl(0, -0.05, 0)));

                // rotations
                if(e.getKeyCode() == KeyEvent.VK_X ){
                    current.setModelMat(current.getModelMat().mul(new Mat4RotX(0.1)));
                }
                if(e.getKeyCode() == KeyEvent.VK_Y ){
                    current.setModelMat(current.getModelMat().mul(new Mat4RotY(0.1)));
                }
                if(e.getKeyCode() == KeyEvent.VK_Z ){
                    current.setModelMat(current.getModelMat().mul(new Mat4RotZ(0.1)));
                }

                // scale
                if(e.getKeyCode() == KeyEvent.VK_EQUALS ){
                    current.setModelMat(current.getModelMat().mul(new Mat4Scale(1.2)));
                }
                if(e.getKeyCode() == KeyEvent.VK_MINUS ){
                    current.setModelMat(current.getModelMat().mul(new Mat4Scale(0.8)));
                }

                // camera control
                if(e.getKeyCode() == KeyEvent.VK_A){
                    camera = camera.left(0.1);
                }
                if(e.getKeyCode() == KeyEvent.VK_D){
                    camera = camera.right(0.1);
                }
                if(e.getKeyCode() == KeyEvent.VK_W){
                    camera = camera.forward(0.1);
                }
                if(e.getKeyCode() == KeyEvent.VK_S){
                    camera = camera.backward(0.1);
                }

                //changing projection mode
                if(e.getKeyCode() == KeyEvent.VK_O){
                    chosenProjection = orthMatrix;
                }
                if(e.getKeyCode() == KeyEvent.VK_P){
                    chosenProjection = perspectiveMatrix;
                }

                // changing the active object3D
                if(e.getKeyCode() == KeyEvent.VK_PERIOD){
                    activeObj = (activeObj +1) % scene.getObjects().size();
                    if(activeObj <= 2 && activeObj >=0){
                        activeObj = 3;
                    }
                }

                // hiding/showing objects
                if(e.getKeyCode() == KeyEvent.VK_C ){
                    cube.changeVisibility();
                }
                if(e.getKeyCode() == KeyEvent.VK_G ){
                    pyramid.changeVisibility();
                }
                if(e.getKeyCode() == KeyEvent.VK_R ){
                    prism.changeVisibility();
                }

                if(e.getKeyCode() == KeyEvent.VK_E){
                    long start = System.currentTimeMillis();
                    Mat4 modelBefore = current.getModelMat();
                    while(System.currentTimeMillis() - start < 2_000){
                        current.setModelMat(current.getModelMat().mul(new Mat4RotXYZ(0.01, 0.01, 0.01)));
                        drawScene();
                    }
                    current.setModelMat(modelBefore);
                }
                drawScene();
            }
        });
    }
    /**
     * initiates variables
     */
    private void setupCanvas(){
        renderer = new Renderer3D();
        x = img.getWidth()/2;
        y = img.getHeight()/2;

        red = new Color(255, 0, 0);
        green = new Color(0, 255, 0);
        blue = new Color(0, 0, 255);

        greenish = new Color(0, 155, 20);
        grey = new Color(47, 47, 47);
        yellow = new Color(255, 255, 0);
        azure = new Color(0, 200, 215);
    }
    /**
     *  scene, camera, projection and orthogonal matrices, and 3D objects initiation
     */
    public void initScene() {
        viewPos = new Vec3D(2, 4, 3);

        camera = new Camera()
                .withPosition(viewPos)
                .withAzimuth(getAzimuthToOrigin(viewPos))
                .withZenith(getZenithToOrigin(viewPos));

        perspectiveMatrix = new Mat4PerspRH(Math.PI/3, 1, 0.1,200);
        orthMatrix = new Mat4OrthoRH(img.getWidth() / 40.0, img.getHeight() / 40.0, -200, 200); // mb change
        chosenProjection = perspectiveMatrix;

        cube = new Cube(new Mat4Identity(), greenish.getRGB());
        pyramid = new Pyramid(new Mat4Transl(3, 3, 3.5), yellow.getRGB());
        prism = new Prism(new Mat4Transl(-0.5, -1, -2), green.getRGB());
        Ox = new Axis(new Point3D(2, 0, 0), red.getRGB());
        Oy = new Axis(new Point3D(0, 2, 0), green.getRGB());
        Oz = new Axis(new Point3D(0, 0, 2), blue.getRGB());

        ArrayList<Object3D> objects = new ArrayList<>();
        objects.add(Ox);
        objects.add(Oy);
        objects.add(Oz);
        objects.add(cube);
        objects.add(pyramid);
        objects.add(prism);
        activeObj = 3;
        scene = new Scene(objects);
    }

    /**
     * draws the scene by clearing it, rendering and presenting
     */
    public void drawScene() {
        clear();
        renderer.render(img, scene, new LineRasterizer(),
                camera.getViewMatrix(),
                chosenProjection);
        panel.repaint();
        img.present(panel.getGraphics());
    }

    /**
     * calculates the azimuth to origin based on given vector
     * @param observerPos Vec3D that represents the observer's position
     * @return azimuth as angle in radians
     */
    private double getAzimuthToOrigin(final Vec3D observerPos){
        final  Vec3D v = observerPos.opposite();
        final double alpha = v.ignoreZ().normalized()
                .map(vNorm -> Math.acos(vNorm.dot(new Vec2D(1,0))))
                .orElse(0.0);
        return (v.getY() > 0) ? alpha : Math.PI*2- alpha;
    }

    /**
     * calculates the zenith to origin based on given vector
     * @param observerPos Vec3D that represents the observer's position
     * @return zenith as angle in radians
     */
    private double getZenithToOrigin(final Vec3D observerPos){
        final  Vec3D v = observerPos.opposite();
        final double alpha = v.normalized()
                .map(vNorm-> Math.acos(vNorm.dot(new Vec3D(0,0,1))))
                .orElse(Math.PI/2);
        return Math.PI/2 - alpha;
    }

    /**
     * Paints the canvas in its background color
     */
    public void clear() {
        img.clear(grey.getRGB());
    }

    public void present(Graphics graphics) {
        img.present(graphics);
    }


    public void start() {
        drawScene();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Canvas3D(800, 600).start());
    }

}
