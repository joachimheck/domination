package org.heckcorp.domination.desktop.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

import org.heckcorp.domination.City;
import org.heckcorp.domination.Constants;
import org.heckcorp.domination.Direction;
import org.heckcorp.domination.GamePiece;
import org.heckcorp.domination.GameView;
import org.heckcorp.domination.Hex;
import org.heckcorp.domination.HexMap;
import org.heckcorp.domination.Player;
import org.heckcorp.domination.Positionable;
import org.heckcorp.domination.ShadowMap;
import org.heckcorp.domination.ShadowStatus;
import org.heckcorp.domination.Status;
import org.heckcorp.domination.Unit;
import org.heckcorp.domination.ViewMonitor;

@SuppressWarnings("serial")
public class SwingView extends JPanel implements GameView
{
    /**
     * Manages the Swing components of this SwingView.
     * 
     * @author Joachim Heck
     */
    public class UIManager extends JPanel {
        public MapView getMapView() {
            return mapView;
        }

        public ImageIcon getUnitIcon(Unit unit) {
            return dataManager.getCounter(unit).getIcon();
        }

        public void message(String message) {
            textArea.insert(message + "\n", textArea.getText().length());
            JScrollBar sb = textScrollPane.getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        }

        /**
         * Creates all the sprites that will be used to display a GamePiecde of
         * the specified type, and sets their position.
         * 
         * @param piece the GamePiece to create a unit for.
         * @return the counter for the GamePiece.
         * @pre piece != null
         */
        private Counter createCounter(GamePiece piece) {
            Point position = new Point(0, 0);

            boolean hidden = true;
            
            if (mapView.isInitialized()) {
                position = mapView.getMapPane().getHexCenter(piece.getPosition());
                hidden = piece.isHidden(displayManager.getShadowMap());
            }
            
            Counter counter = new Counter(piece, position);
            counter.setHidden(hidden);

            return counter;
        }

        public UIManager() {
            mapView = new MapView();

            hexDescriptionPanel = new HexDescriptionPanel(this);
            textArea = new JTextArea();
            textArea.setRows(5);
            textArea.setEditable(false);
            textArea.setFocusable(false);
            textScrollPane = new JScrollPane(textArea);
            textScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            miniMap = new MiniMap(getMap());
            miniMap.setMinimumSize(new Dimension(Constants.UI_COMPONENT_SMALL_WIDTH,
                                                 Constants.UI_COMPONENT_SMALL_HEIGHT));
            mapView.addMapViewListener(miniMap);

            SpringLayout layout = new SpringLayout();
            setLayout(layout);
            
            add(mapView);
            add(hexDescriptionPanel);
            add(textScrollPane);
            add(miniMap);
            
            int smWidth = Constants.UI_COMPONENT_SMALL_WIDTH;
            int smHeight = Constants.UI_COMPONENT_SMALL_HEIGHT;
            int lgWidth = Constants.UI_COMPONENT_LARGE_WIDTH;
            int lgHeight = Constants.UI_COMPONENT_LARGE_HEIGHT;
            
            mapView.setPreferredSize(new Dimension(lgWidth, lgHeight));
            mapView.setMinimumSize(new Dimension(smWidth, smHeight));
            mapView.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            hexDescriptionPanel.setPreferredSize(new Dimension(smWidth, lgHeight));
            hexDescriptionPanel.setMaximumSize(new Dimension(smWidth, Integer.MAX_VALUE));
            textScrollPane.setPreferredSize(new Dimension(lgWidth, smHeight));
            textScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, smHeight));
            miniMap.setPreferredSize(new Dimension(smWidth, smHeight));
            miniMap.setMaximumSize(new Dimension(smWidth, smHeight));
            miniMap.setBorder(BorderFactory.createLineBorder(Color.black));
            
            SpringLayout.Constraints panelCons = layout.getConstraints(this);
            SpringLayout.Constraints mapViewCons = layout.getConstraints(mapView);
            SpringLayout.Constraints hexCons = layout.getConstraints(hexDescriptionPanel);
            SpringLayout.Constraints textCons = layout.getConstraints(textScrollPane);
            SpringLayout.Constraints miniMapCons = layout.getConstraints(miniMap);

            mapViewCons.setX(Spring.constant(5));
            mapViewCons.setY(Spring.constant(5));

            hexCons.setX(Spring.sum(Spring.constant(5), layout.getConstraint(SpringLayout.EAST, mapView)));
            hexCons.setY(Spring.constant(5));
            hexCons.setWidth(Spring.constant(smWidth));
            hexCons.setHeight(mapViewCons.getHeight());
            
            textCons.setX(Spring.constant(5));
            textCons.setY(Spring.sum(layout.getConstraint(SpringLayout.SOUTH, mapView), Spring.constant(5)));
            textCons.setWidth(mapViewCons.getWidth());
            textCons.setHeight(Spring.constant(smHeight));
            
            miniMapCons.setX(hexCons.getX());
            miniMapCons.setY(textCons.getY());
            miniMapCons.setWidth(hexCons.getWidth());
            miniMapCons.setHeight(textCons.getHeight());
            
            panelCons.setWidth(Spring.sum(layout.getConstraint(SpringLayout.EAST, hexDescriptionPanel),
                                          Spring.constant(5)));
            panelCons.setHeight(Spring.sum(layout.getConstraint(SpringLayout.SOUTH, textScrollPane),
                                          Spring.constant(5)));
        }

        private HexDescriptionPanel hexDescriptionPanel;

        private MapView mapView;

        private MiniMap miniMap;

        private JTextArea textArea;

        private JScrollPane textScrollPane;

        public MiniMap getMiniMap() {
            return miniMap;
        }

        public void initialize(HexMap map, ShadowMap shadowMap, ViewMonitor monitor) {
            mapView.initialize(map, shadowMap, monitor);
            miniMap.initialize(map, shadowMap);
        }

        public void clearTextArea() {
            textArea.setText(null);
        }

    }

    /**
     * Manages the display of game actions such as unit movement.
     * 
     * @author Joachim Heck
     */
    private class DisplayManager implements Observer {
        public void drawAll() {
            invokeAndWait(new Runnable() {
                public void run() {
                    mapView.invalidate();
                    miniMap.invalidate();
                }
            });
        }

        public void hideAttackArrow() {
            invokeAndWait(new Runnable() {
                public void run() {
                    resources.getAttackArrow().setHidden(true);
                }
            });
        }

        public void hideSelection() {
            invokeAndWait(new Runnable() {
                public void run() {
                    resources.getSelection().setHidden(true);
                }
            });
        }

        /**
         * Moves the counter to the specified hex.
         * @param counter
         * @param destHex
         */
        public void moveCounter(final Counter counter, final Hex destHex) {
            MapPane mapPane = mapView.getMapPane();
            Point position = mapPane.getHexCenter(destHex.getPosition());
            counter.addObserver(DisplayManager.this);
            counter.moveCenterTo(position);

            try {
                // The counter will inform us when it's finished moving.
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                // Ignore.
            }

            counter.deleteObserver(DisplayManager.this);
            counter.setMapPosition(destHex.getPosition());
            resources.getSelection().setMapPosition(destHex.getPosition());
            resources.getSelection().setHidden(counter.isHidden());
            
            miniMap.invalidate();
        }

        public void moveToFront(final Counter counter) {
            invokeAndWait(new Runnable() {
                public void run() {
                    mapView.moveToFront(counter);
                }
            });
        }

        private Counter selectedCounter;

        /**
         * @param counter
         * @param status
         * @pre counter != null
         * @pre status != null
         */
        public void setCounterStatus(final Counter counter, final Status status) {
            if (status == Status.DESTROYED) {
                Counter explosion = UIResources.getInstance().getExplosion();

                explosion.setCenterLocation(counter.getCenterLocation());
                explosion.setCurrentFrame(0);
                explosion.setAnimated(true);
                explosion.setHidden(false);

                try {
                    // The sprite will inform us when it's finished animating.
                    synchronized(this) {
                        wait();
                    }

                    explosion.setHidden(true);
                } catch (InterruptedException e) {
                    // Ignore.
                }
                
                miniMap.invalidate();
            } else if (status == Status.DAMAGED) {
                counter.setDamaged(true);
                pause(Constants.PAUSE_TIME);
            } else { 
                invokeAndWait(new Runnable() {
                    public void run() {
                        if (status == Status.DAMAGED) {
                            // We dealt with this status earlier in the method.
                            assert false;
                        } else if (status == Status.DESTROYED) {
                            // We dealt with this status earlier in the method.
                            assert false;
                        } else if (status == Status.HEALTHY) {
                            counter.setDamaged(false);
                        } else if (status == Status.HIDDEN) {
                            counter.setHidden(true);
                            miniMap.invalidate();
                        } else if (status == Status.REVEALED) {
                            counter.setHidden(false);
                            miniMap.invalidate();
                        } else if (status == Status.SELECTED) {
                            selectedCounter = counter;

                            if (!counter.isHidden()) {
                                Point point = new Point(counter.getMapPosition());
                                point.translate(-1, -1);
                                Rectangle rect =
                                    new Rectangle(point, new Dimension(2, 2));
                                mapView.centerRectangle(rect);
                                mapView.moveToFront(counter);
                            }

                            Counter selection = UIResources.getInstance().getSelection();
                            assert selection.getMapPosition() == null;
                            selection.setHidden(counter.isHidden());
                            selection.setOnScreen(!counter.isHidden());
                            selection.setAnimated(!counter.isHidden());
                            selection.setMapPosition(counter.getMapPosition());
                            selection.setCenterLocation(counter.getCenterLocation());
                        } else if (status == Status.SKIPPED) {
                        } else if (status == Status.UNSELECTED) {
                            Counter selection = UIResources.getInstance().getSelection();

                            assert selection.getMapPosition() != null;
                            selection.setMapPosition(null);
                            selection.setHidden(true);
                            selection.setAnimated(false);
                        } else {
                            assert false : "Unknown unit status!";
                        }
                    }
                });
            }
        }

        /**
         * Sets the visibility status flags on all counters to insure that all
         * counters in visible portions of the map are visible and all others
         * are hidden.
         * 
         * @param shadowMap
         * @return
         * 
         * @pre shadowMap != null
         */
        public void setCounterVisibility(final ShadowMap shadowMap) {
            invokeAndWait(new Runnable() {
                public void run() {
                    for (Counter counter : viewDataManager.getCounters()) {
                        counter.setHidden(counter.isHidden(shadowMap));
                    }
                    
                    miniMap.invalidate();
                }
            });
        }

        public void showAttackArrow(final Hex hexFrom, final Hex hexTo) {
            invokeAndWait(new Runnable() {
                public void run() {
                    final Direction d = HexMap.getDirection(hexFrom.getPosition(),
                                                            hexTo.getPosition());
                    Point pFrom = mapView.getMapPane().getHexCenter(hexFrom.getPosition());
                    Point pTo = mapView.getMapPane().getHexCenter(hexTo.getPosition());
                    final Point center = new Point(pFrom.x + (pTo.x - pFrom.x)/2,
                                                   pFrom.y + (pTo.y - pFrom.y)/2);
                    
                    Counter attackArrow = UIResources.getInstance().getAttackArrow();
                    attackArrow.setCurrentFrame(d.ordinal());
                    attackArrow.setCenterLocation(center);
                    attackArrow.setHidden(false);
                }
            });
        }

        public void showCounter(final Counter counter) {
            invokeAndWait(new Runnable() {
                public void run() {
                    counter.setHidden(false);
                    counter.revalidate();
                    counter.repaint();
                    miniMap.invalidate();
                }
            });
        }

        public synchronized void update(Observable o, Object state) {
            if (state == ObservableState.State.FINISHED_ANIMATING ||
                state == ObservableState.State.FINISHED_MOVING)
            {
                notify();
            } else if (state == ObservableState.State.MOVING ||
                       state == ObservableState.State.ANIMATING)
            {
                // TODO: revalidate?
                repaint();
            } else {
                // Unknown observable reporting.
                assert false;
            }
        }

        public void updateShadowMap(ShadowMap shadowMap) {
            invokeAndWait(new Runnable() {
                public void run() {
                    mapView.invalidate();
                    miniMap.invalidate();
                }
            });
        }

        /**
         * Invokes the run() method of runnable on the AWT event dispatching thread,
         * and swallows any runtime exceptions that are generated.
         * 
         * @param runnable
         */
        private void invokeAndWait(Runnable runnable) {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InterruptedException e) {
                // Ignore - what else can I do?
            } catch (InvocationTargetException e) {
                // TODO: throw a new RuntimeException here maybe?
                e.printStackTrace();
                assert false;
            }
        }

        public void relocateCounters() {
            for (Counter counter : dataManager.getCounters()) {
                GamePiece piece = dataManager.getGamePiece(counter);
                Point hexCenter = uiManager.getMapView().getMapPane().
                    getHexCenter(piece.getPosition());
                counter.setCenterLocation(hexCenter);
                counter.setHidden(!piece.isHidden(getShadowMap()));
            }
            
            if (selectedCounter != null) {
                resources.getSelection().setLocation(selectedCounter.getLocation());
            }
            
            miniMap.invalidate();
        }

        public DisplayManager(MapView mapView, MiniMap miniMap,
                              ViewDataManager viewDataManager)
        {
            this.mapView = mapView;
            this.miniMap = miniMap;
            this.viewDataManager = viewDataManager;
            
            resources.getExplosion().addObserver(this);
            resources.getSelection().addObserver(this);
            resources.getAttackArrow().addObserver(this);

        }

        private final MapView mapView;
        private final MiniMap miniMap;
        private final ViewDataManager viewDataManager;
        private ShadowMap shadowMap = null;

        public void setShadowMap(ShadowMap shadowMap) {
            this.shadowMap = shadowMap;
        }
        
        public ShadowMap getShadowMap() {
            return shadowMap;
        }

        public void pause(final int millis) {
            invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            });
        }

        /**
         * @pre first and second are adjacent
         * @param first
         * @param second
         */
        public void displayPositions(Positionable first, Positionable second) {
            if (shadowMap.isVisible(first.getPosition()) ||
                shadowMap.isVisible(second.getPosition()))
            {
                // Make sure all the hexes surrounding the start point and all
                // those surrounding the end point are visible.
                Set<Hex> adjacent = map.getAdjacentHexes(first);
                adjacent.addAll(map.getAdjacentHexes(second));
                
                Point min = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
                Point max = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
                
                for (Hex hex : adjacent) {
                    if (hex.getPosition().x < min.x) {
                        min.x = hex.getPosition().x;
                    }
                    if (hex.getPosition().y < min.y) {
                        min.y = hex.getPosition().y;
                    }
                    if (hex.getPosition().x > max.x) {
                        max.x = hex.getPosition().x;
                    }
                    if (hex.getPosition().y > max.y) {
                        max.y = hex.getPosition().y;
                    }
                }
                
                Dimension size = new Dimension(max.x - min.x, max.y - min.y);
                
                mapView.centerRectangle(new Rectangle(min, size));
            }

        }
    }

    /**
     * Manages the non-graphical information for this SwingView, in particular
     * the mapping between GamePieces and Counters.
     * 
     * @author Joachim Heck
     */
    private class ViewDataManager {
        public void addPiece(GamePiece piece, Counter counter) {
//            System.out.println("Adding game piece: " + piece);
            
            counters.put(piece, counter);
            pieces.put(counter, piece);
        }

        public Set<Counter> getCounters() {
            return pieces.keySet();
        }

        public GamePiece getGamePiece(Counter counter) {
            return pieces.get(counter);
        }

        public Counter getCounter(GamePiece piece) {
            return counters.get(piece);
        }

        private Map<GamePiece, Counter> counters = new HashMap<GamePiece, Counter>();
        private Map<Counter, GamePiece> pieces = new HashMap<Counter, GamePiece>();
        public Set<GamePiece> getGamePieces() {
            return counters.keySet();
        }
    }

    public void addGamePiece(GamePiece piece) {
        log.finest("Adding piece: " + piece);
        Counter counter = uiManager.createCounter(piece);
        dataManager.addPiece(piece, counter);
        uiManager.getMapView().add(counter, MapView.SPRITE_LAYER);
        revalidate();
    }

    /**
     * Initiates an attack.
     * 
     * @param attacker
     * @param target
     * @pre unit != null
     * @pre hex != null
     * @pre unit has been added to this view, and not destroyed
     * @pre unit.canAttack(hex)
     */
    public void attack(final Unit attacker, final Unit target) {
        final Counter targetCounter = dataManager.getCounter(target);

        displayManager.displayPositions(attacker, target);
        displayManager.moveToFront(targetCounter);
        displayManager.showAttackArrow(attacker.getHex(),
                                       target.getHex());
        displayManager.pause(Constants.PAUSE_TIME);
        displayManager.hideAttackArrow();
    }

    // TODO: move out of swing view, or at least add another one
    // to uiManager or something, for debugging messages from
    // the view components.
    public void message(String message) {
        uiManager.message(message);
    }

    /**
     * Moves the unit one hex. This method assumes the unit's position has been
     * updated already, so it uses the unit's last hex as the starting point for
     * the move.
     * 
     * @param unit
     * @param direction
     * 
     * @pre unit != null
     * @pre direction != null
     * @pre unit has been added to this view, and not destroyed
     */
    public void move(final Unit unit, final Direction direction) {
        Hex destHex = map.getAdjacentHex(unit.getLastHex(), direction);
        displayManager.hideSelection();

        ShadowMap shadowMap = displayManager.getShadowMap();

        Counter counter = dataManager.getCounter(unit);
        if (shadowMap.isVisible(unit.getLastHex().getPosition())) {
            displayManager.showCounter(counter);
            // displayManager.pause();
        }

        displayManager.displayPositions(unit.getLastHex(), unit.getHex());
        displayManager.moveCounter(counter, unit.getHex());
        displayManager.updateShadowMap(shadowMap);
        displayManager.setCounterVisibility(shadowMap);

        City city = destHex.getCity();
        if (city != null) {
            // Make sure the city has the right player's color.
            Counter cityCounter = dataManager.getCounter(city);
            cityCounter.setBorderColor(unit.getOwner().getColor());
        }

        counter.setHidden(unit.isHidden(shadowMap));
        if (!counter.isHidden()) {
            uiManager.hexDescriptionPanel.setHex(destHex);
        }
    }

    /**
     * Selects the specified hex. If a unit or another hex is currently
     * selected, it is first unselected.
     * 
     * @param hex
     * @pre hex != null
     */
    public void selectHex(Hex hex) {
        if (currentPlayer == mainPlayer) {
            ShadowStatus status =
                displayManager.getShadowMap().getStatus(hex.getPosition());

            uiManager.hexDescriptionPanel.setHex(hex, status);
        }
    }

    /**
     * Sets this view's monitor.  The view will update the
     * controller whenever the viewport size or position changes.
     * @param monitor
     * @pre monitor != null
     */
    public void setMonitor(ViewMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Sets this view's current player to the specified player.
     * 
     * @param player
     * @pre player != null
     */
    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
        monitor.setViewingPlayerActive(player == mainPlayer);
        uiManager.clearTextArea();
        message("Now moving: " + player.getName());
        // TODO: redraw current player label.
    }

    /**
     * Sets the shadow map to be displayed in this view.
     * @param shadowMap
     * @pre shadowMap != null
     * @pre setMap() must have already been called.
     * @pre shadowMap().getSize() == size of this view's current map.
     * @pre this view's shadow map must not have already been set.
     */
    public void setShadowMap(ShadowMap shadowMap) {
        log.finer("Setting shadow map: " + shadowMap);
        assert displayManager.getShadowMap() == null;
        
        uiManager.initialize(map, shadowMap, monitor);
        displayManager.setShadowMap(shadowMap);
        displayManager.relocateCounters();
    }

    /**
     * Sets the map to be displayed in this view.
     * 
     * @param map
     * @pre map != null
     * @pre this view's map is null.
     */
    public void setMap(HexMap map) {
        log.finer("Setting map: " + map);
        this.map = map;
    }

    /**
     * Sets the shadow map to be displayed in this view.
     * 
     * @param shadowMap
     * @pre shadowMap != null
     * @pre setMap() must have already been called.
     * @pre shadowMap().getSize() == size of this view's map.
     */
//    public void setShadowMap(ShadowMap shadowMap) {
//        assert map != null;
//        assert shadowMap != null;
//        assert shadowMap.getSize().equals(map.getSize());
//
//        this.shadowMap = shadowMap;
//        displayManager.drawAll();
//    }

    /**
     * Sets a status attribute of the unit.
     * 
     * @param unit
     * @param status
     * @pre unit != null
     * @pre unit has been added to this view, and not destroyed.
     */
    public void setStatus(Unit unit, Status status) {
        Counter counter = dataManager.getCounter(unit);
        assert counter != null : "No counter for " + unit;
        displayManager.setCounterStatus(counter, status);
        
        if (status == Status.DESTROYED) {
            uiManager.getMapView().removeCounter(counter);
        }
        
        if (currentPlayer != mainPlayer) {
            // TODO: advance some kind of clock icon.
        }
    }

    public void setWinningPlayer(Player winner) {
        assert winner != null;

        BufferedImage tempImage = new BufferedImage(500, 50,
                                                    BufferedImage.TRANSLUCENT);
        Graphics2D g = (Graphics2D) tempImage.getGraphics();

        String winnerString = winner.getName() + " wins!";
        g.setFont(g.getFont().deriveFont(36.0f));
        g.setColor(winner.getColor());
        Rectangle2D bounds = g.getFont().getStringBounds(winnerString,
                                                         g.getFontRenderContext());
        g.drawString(winnerString, (int) -bounds.getX(), (int) -bounds.getY());

        BufferedImage winnerImage = new BufferedImage((int) bounds.getWidth(),
                                                      (int) bounds.getHeight(),
                                                      BufferedImage.TRANSLUCENT);
        Graphics g2 = winnerImage.getGraphics();
//        g2.fillRect(0, 0, winnerImage.getWidth(), winnerImage.getHeight());
        g2.drawImage(tempImage, 0, 0, winnerImage.getWidth(),
                     winnerImage.getWidth(), 0, 0, winnerImage.getWidth(),
                     winnerImage.getWidth(), this);
        JLabel winnerLabel = new JLabel(new ImageIcon(winnerImage));

        getRootPane().setGlassPane(winnerLabel);
        winnerLabel.setVisible(true);

//        repaint();
    }
    
    private HexMap getMap() {
        return map;
    }

    public SwingView() {
        // TODO: add these in as some kind of plug-ins, maybe?
        uiManager = new UIManager();
        dataManager = new ViewDataManager();
        displayManager = new DisplayManager(uiManager.getMapView(),
                                            uiManager.getMiniMap(),
                                            dataManager);
        
        setLayout(new BorderLayout());
        add(uiManager);
        
        // When loading a game, the selection may have a position.
        Counter selection = UIResources.getInstance().getSelection();
        selection.setMapPosition(null);
        selection.setHidden(true);
        selection.setAnimated(false);
        
        log = Logger.getLogger(getClass().getName());
    }

    private Logger log;
    private ViewMonitor monitor;

    private Player currentPlayer;

    private ViewDataManager dataManager;

    private DisplayManager displayManager;

    private HexMap map;

    private UIResources resources = UIResources.getInstance();

    private UIManager uiManager;
    
    private Player mainPlayer = null;

    public static SwingView getInstance() {
        if (instance == null) {
            instance = new SwingView();
        }

        return instance;
    }

    private static SwingView instance = null;

    public void setMainPlayer(Player mainPlayer) {
        assert mainPlayer != null;
        assert this.mainPlayer == null;
        this.mainPlayer = mainPlayer;
    }
}
