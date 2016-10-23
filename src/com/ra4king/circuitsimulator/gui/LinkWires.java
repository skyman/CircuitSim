package com.ra4king.circuitsimulator.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ra4king.circuitsimulator.gui.Connection.PortConnection;
import com.ra4king.circuitsimulator.gui.Connection.WireConnection;
import com.ra4king.circuitsimulator.simulator.CircuitState;
import com.ra4king.circuitsimulator.simulator.Port;
import com.ra4king.circuitsimulator.simulator.Port.Link;
import com.ra4king.circuitsimulator.simulator.WireValue;
import com.ra4king.circuitsimulator.simulator.utils.Pair;

/**
 * @author Roi Atalla
 */
public class LinkWires {
	private Set<PortConnection> ports;
	private Set<Wire> wires;
	
	public LinkWires() {
		ports = new HashSet<>();
		wires = new HashSet<>();
	}
	
	public Link getLink() {
		return ports.size() > 0 ? ports.iterator().next().getLink() : null;
	}
	
	public void addWire(Wire wire) {
		wires.add(wire.getLinkWires() != this ? new Wire(wire) : wire);
	}
	
	public Set<Wire> getWires() {
		return wires;
	}
	
	public List<LinkWires> removeWire(Wire wire) {
		if(!wires.contains(wire)) {
			return Collections.singletonList(this);
		}
		
		wires.remove(wire);
		
		Link link = getLink();
		if(link != null) {
			for(PortConnection portConnection : ports) {
				link.unlinkPort(portConnection.getPort());
			}
		}
		
		List<LinkWires> newLinkWires = new ArrayList<>();
		
		while(wires.size() != 0) {
			Wire nextWire = wires.iterator().next();
			wires.remove(nextWire);
			
			Pair<List<Wire>, List<PortConnection>> attachedConnections = findAttachedConnections(nextWire);
			
			LinkWires linkWires = new LinkWires();
			linkWires.addWire(nextWire);
			attachedConnections.first.forEach(linkWires::addWire);
			attachedConnections.second.forEach(linkWires::addPort);
			newLinkWires.add(linkWires);
		}
		
		return newLinkWires;
	}
	
	private Pair<List<Wire>, List<PortConnection>> findAttachedConnections(Wire wire) {
		List<Wire> attachedWires = new ArrayList<>();
		List<PortConnection> attachedPorts = new ArrayList<>();
		
		Connection start = wire.getConnections().get(0);
		Connection end = wire.getConnections().get(wire.getConnections().size() - 1);
		for(Iterator<Wire> iter = wires.iterator(); iter.hasNext();) {
			Wire w = iter.next();
			
			boolean added = false;
			
			for(Connection c : w.getConnections()) {
				if((c.getX() == start.getX() && c.getY() == start.getY()) ||
						   (c.getX() == end.getX() && c.getY() == end.getY())) {
					attachedWires.add(w);
					iter.remove();
					added = true;
					break;
				}
			}
			
			if(!added) {
				Connection wStart = w.getConnections().get(0);
				Connection wEnd = w.getConnections().get(w.getConnections().size() - 1);
				
				for(Connection c : wire.getConnections()) {
					if((c.getX() == wStart.getX() && c.getY() == wStart.getY()) ||
							   (c.getX() == wEnd.getX() && c.getY() == wEnd.getY())) {
						attachedWires.add(w);
						iter.remove();
						break;
					}
				}
			}
		}
		
		for(Wire attachedWire : new ArrayList<>(attachedWires)) {
			Pair<List<Wire>, List<PortConnection>> attachedConnections = findAttachedConnections(attachedWire);
			attachedWires.addAll(attachedConnections.first);
			attachedPorts.addAll(attachedConnections.second);
		}
		
		for(Iterator<PortConnection> iter = ports.iterator(); iter.hasNext();) {
			PortConnection port = iter.next();
			
			for(Connection c : wire.getConnections()) {
				if(port.getX() == c.getX() && port.getY() == c.getY()) {
					attachedPorts.add(port);
					iter.remove();
					break;
				}
			}
		}
		
		return new Pair<>(attachedWires, attachedPorts);
	}
	
	public void addPort(PortConnection port) {
		if(ports.size() > 0) {
			Link link = getLink();
			if(port.getLink() != link) {
				link.linkPort(port.getPort());
			}
		}
		
		ports.add(port);
	}
	
	public Set<PortConnection> getPorts() {
		return ports;
	}
	
	public void removePort(PortConnection port) {
		if(!ports.contains(port))
			return;
		
		getLink().unlinkPort(port.getPort());
		ports.remove(port);
	}
	
	public LinkWires merge(LinkWires other) {
		other.ports.forEach(this::addPort);
		other.wires.forEach(this::addWire);
		return this;
	}
	
	public void paint(Graphics2D g, CircuitState circuitState) {
		for(Wire wire : wires) {
			wire.paint(g, circuitState);
		}
	}
	
	public class Wire extends GuiElement {
		private int length;
		private boolean horizontal;
		private List<Connection> connections = new ArrayList<>();
		
		public Wire(Wire wire) {
			this(wire.getX(), wire.getY(), wire.length, wire.horizontal);
		}
		
		public Wire(int startX, int startY, int length, boolean horizontal) {
			super(startX, startY, horizontal ? Math.abs(length) : 2, horizontal ? 2 : Math.abs(length));
			
			if(length == 0)
				throw new IllegalArgumentException("Length cannot be 0");
			
			if(length < 0) {
				if(horizontal) {
					setX(startX + length);
				} else {
					setY(startY + length);
				}
				
				this.length = length = -length;
			} else {
				this.length = length;
			}
			
			this.horizontal = horizontal;
			
			int count = Math.max(1, length / GuiUtils.BLOCK_SIZE);
			int xOffset = horizontal ? GuiUtils.BLOCK_SIZE : 0;
			int yOffset = horizontal ? 0 : GuiUtils.BLOCK_SIZE;
			for(int i = 0; i < count; i++) {
				connections.add(new WireConnection(this, i * xOffset, i * yOffset));
			}
			connections.add(new WireConnection(this, length * (xOffset != 0 ? 1 : 0), length * (yOffset != 0 ? 1 : 0)));
		}
		
		public LinkWires getLinkWires() {
			return LinkWires.this;
		}
		
		public int getLength() {
			return length;
		}
		
		public boolean isHorizontal() {
			return horizontal;
		}
		
		@Override
		public List<Connection> getConnections() {
			return connections;
		}
		
		@Override
		public int hashCode() {
			return getX() ^ getY() ^ (horizontal ? 1 : 0) ^ length;
		}
		
		@Override
		public boolean equals(Object other) {
			if(other instanceof Wire) {
				Wire wire = (Wire)other;
				return this.getX() == wire.getX() && this.getY() == wire.getY() && this.horizontal == wire.horizontal && this.length == wire.length;
			}
			
			return false;
		}
		
		@Override
		public void paint(Graphics2D g, CircuitState circuitState) {
			g.setStroke(new BasicStroke(2));
			if(ports.size() > 0) {
				Port port = ports.iterator().next().getPort();
				if(circuitState.isShortCircuited(port.getLink())) {
					g.setColor(Color.RED);
				}
				GuiUtils.setBitColor(g, circuitState.getValue(port));
			} else {
				GuiUtils.setBitColor(g, new WireValue(1));
			}
			g.drawLine(getX(), getY(), horizontal ? getX() + length : getX(), horizontal ? getY() : getY() + length);
		}
	}
}
