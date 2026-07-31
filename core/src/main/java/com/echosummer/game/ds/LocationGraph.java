package com.echosummer.game.ds;

import com.echosummer.game.ds.adt.IGraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Custom Graph data structure using adjacency lists to represent map location connectivity
 * and navigation paths in Echo Summer.
 *
 * @param <V> Vertex type representing locations
 */
public class LocationGraph<V> implements IGraph<V> {

    public static class Edge<V> {
        private final V target;
        private final float weight;

        public Edge(V target, float weight) {
            this.target = target;
            this.weight = weight;
        }

        public V getTarget() {
            return target;
        }

        public float getWeight() {
            return weight;
        }
    }

    private final Map<V, List<Edge<V>>> adjacencyMap;

    public LocationGraph() {
        this.adjacencyMap = new HashMap<>();
    }

    @Override
    public boolean addVertex(V vertex) {
        if (vertex == null || adjacencyMap.containsKey(vertex)) {
            return false;
        }
        adjacencyMap.put(vertex, new ArrayList<>());
        return true;
    }

    @Override
    public void addEdge(V u, V v) {
        addEdge(u, v, 1.0f, true);
    }

    @Override
    public void addEdge(V u, V v, float weight, boolean bidirectional) {
        addVertex(u);
        addVertex(v);

        adjacencyMap.get(u).add(new Edge<>(v, weight));
        if (bidirectional) {
            adjacencyMap.get(v).add(new Edge<>(u, weight));
        }
    }

    @Override
    public List<V> getNeighbors(V vertex) {
        List<V> neighbors = new ArrayList<>();
        List<Edge<V>> edges = adjacencyMap.get(vertex);
        if (edges != null) {
            for (Edge<V> edge : edges) {
                neighbors.add(edge.getTarget());
            }
        }
        return neighbors;
    }

    @Override
    public boolean hasEdge(V u, V v) {
        List<Edge<V>> edges = adjacencyMap.get(u);
        if (edges != null) {
            for (Edge<V> edge : edges) {
                if (edge.getTarget().equals(v)) return true;
            }
        }
        return false;
    }

    @Override
    public List<V> getVertices() {
        return new ArrayList<>(adjacencyMap.keySet());
    }

    @Override
    public int vertexCount() {
        return adjacencyMap.size();
    }

    /**
     * Computes the shortest navigation path between start and target location using Breadth-First Search (BFS).
     */
    public List<V> findShortestPathBFS(V start, V target) {
        List<V> path = new ArrayList<>();
        if (!adjacencyMap.containsKey(start) || !adjacencyMap.containsKey(target)) {
            return path;
        }

        Map<V, V> parentMap = new HashMap<>();
        Queue<V> queue = new LinkedList<>();
        List<V> visited = new ArrayList<>();

        queue.add(start);
        visited.add(start);
        parentMap.put(start, null);

        boolean found = false;
        while (!queue.isEmpty()) {
            V curr = queue.poll();
            if (curr.equals(target)) {
                found = true;
                break;
            }

            for (V neighbor : getNeighbors(curr)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, curr);
                    queue.add(neighbor);
                }
            }
        }

        if (found) {
            V curr = target;
            while (curr != null) {
                path.add(0, curr);
                curr = parentMap.get(curr);
            }
        }

        return path;
    }
}
