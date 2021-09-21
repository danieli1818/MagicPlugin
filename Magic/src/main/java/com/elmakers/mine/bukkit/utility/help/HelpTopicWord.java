package com.elmakers.mine.bukkit.utility.help;

import com.elmakers.mine.bukkit.ChatUtils;

public class HelpTopicWord {
    public static double RARITY_FACTOR = 0.6;
    public static double TOPIC_RARITY_FACTOR = 1.8;
    public static double LENGTH_FACTOR = 0.2;
    public static double RARITY_WEIGHT = 2;
    public static double TOPIC_RARITY_WEIGHT = 1.2;
    public static double LENGTH_WEIGHT = 1.2;

    private final String word;
    private int count;
    private int topicCount;
    private Double weight;

    public HelpTopicWord(String word) {
        this.word = word;
    }

    public int getCount() {
        return count;
    }

    public int getTopicCount() {
        return topicCount;
    }

    public void addTopic(int count) {
        this.topicCount++;
        this.count += count;
    }

    public double getWeight(Help help) {
        if (weight == null) {
            weight = computeWeight(help);
        }
        return weight;
    }

    private double computeWeight(Help help) {
        double rarityWeight = getRarityWeight(help.maxCount) * RARITY_WEIGHT;
        double topicRarityWeight = getTopicWeight(help.maxTopicCount) * TOPIC_RARITY_WEIGHT;
        double lengthWeight = getLengthWeight(word, help.maxLength) * LENGTH_WEIGHT;
        double totalWeight = RARITY_WEIGHT + TOPIC_RARITY_WEIGHT + LENGTH_WEIGHT;
        return (rarityWeight + topicRarityWeight + lengthWeight) / totalWeight;
    }

    protected double getRarityWeight(int maxCount) {
        double rarityWeight = 1.0 - ((double)count / (maxCount + 1));
        return Math.pow(rarityWeight, RARITY_FACTOR);
    }

    protected double getLengthWeight(String word, int maxLength) {
        double lengthWeight = (double)word.length() / maxLength;
        return Math.pow(lengthWeight, LENGTH_FACTOR);
    }

    protected double getTopicWeight(int maxTopicCount) {
        double topicRarityWeight = 1.0 - ((double)topicCount / (maxTopicCount + 1));
        return Math.pow(topicRarityWeight, TOPIC_RARITY_FACTOR);
    }

    public String getDebugText(Help help) {
        double rarityWeight = getRarityWeight(help.maxCount);
        double topicRarityWeight = getTopicWeight(help.maxTopicCount);
        double lengthWeight = getLengthWeight(word, help.maxLength);
        return "Rare: "
                + ChatUtils.printPercentage(rarityWeight)
                + "x" + (int)RARITY_WEIGHT
                + " + TRare: "
                + ChatUtils.printPercentage(topicRarityWeight)
                + "x" + (int)TOPIC_RARITY_WEIGHT
                + " + Len: "
                + ChatUtils.printPercentage(lengthWeight)
                + "x" + (int)LENGTH_WEIGHT;
    }

    public void reset() {
        this.weight = null;
    }
}
